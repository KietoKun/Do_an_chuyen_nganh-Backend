package com.pizzastore.service;

import com.pizzastore.dto.OrderRequest;
import com.pizzastore.dto.StaffOrderRequest;
import com.pizzastore.entity.*;
import com.pizzastore.enums.DeliveryMethod;
import com.pizzastore.enums.OrderSource;
import com.pizzastore.enums.OrderStatus;
import com.pizzastore.enums.RoleName;
import com.pizzastore.repository.*;
import com.pizzastore.util.LocationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final DishVariantRepository dishVariantRepository;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final ToppingRepository toppingRepository;
    private final CouponRepository couponRepository;
    private final EmployeeRepository employeeRepository;
    private final BranchRepository branchRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryBatchRepository inventoryBatchRepository;
    private final InventoryBatchConsumptionRepository inventoryBatchConsumptionRepository;
    private final OrderRealtimeService orderRealtimeService;
    private final MenuAvailabilityRealtimeService menuAvailabilityRealtimeService;
    private final BranchAccessService branchAccessService;
    private final PasswordEncoder passwordEncoder;
    private static final List<OrderStatus> PENDING_COOK_SLOT_STATUSES = List.of(
            OrderStatus.PENDING,
            OrderStatus.PAID,
            OrderStatus.CONFIRMED
    );
    private static final double DEFAULT_MAX_SERVICE_RADIUS_KM = 7.0;
    private static final int DEFAULT_MAX_PENDING_COOK_ORDERS = 10;

    @Value("${goong.api.key}")
    private String goongApiKey;

    @Autowired
    public OrderService(OrderRepository orderRepository,
                        DishVariantRepository dishVariantRepository,
                        CustomerRepository customerRepository,
                        AccountRepository accountRepository,
                        ProductRepository productRepository,
                        DishService dishService,
                        ToppingRepository toppingRepository,
                        CouponRepository couponRepository,
                        EmployeeRepository employeeRepository,
                        BranchRepository branchRepository,
                        InventoryRepository inventoryRepository,
                        InventoryBatchRepository inventoryBatchRepository,
                        InventoryBatchConsumptionRepository inventoryBatchConsumptionRepository,
                        OrderRealtimeService orderRealtimeService,
                        MenuAvailabilityRealtimeService menuAvailabilityRealtimeService,
                        BranchAccessService branchAccessService,
                        PasswordEncoder passwordEncoder) {
        this.orderRepository = orderRepository;
        this.dishVariantRepository = dishVariantRepository;
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.toppingRepository = toppingRepository;
        this.couponRepository = couponRepository;
        this.employeeRepository = employeeRepository;
        this.branchRepository = branchRepository;
        this.inventoryRepository = inventoryRepository;
        this.inventoryBatchRepository = inventoryBatchRepository;
        this.inventoryBatchConsumptionRepository = inventoryBatchConsumptionRepository;
        this.orderRealtimeService = orderRealtimeService;
        this.menuAvailabilityRealtimeService = menuAvailabilityRealtimeService;
        this.branchAccessService = branchAccessService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Order createOrder(String username, OrderRequest request) {
        validateOrderRequest(request);
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));

        Customer customer = customerRepository.findAll().stream()
                .filter(c -> c.getAccount().getId().equals(account.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin khách hàng"));

        Order order = new Order();
        order.setCustomer(customer);
        order.setOrderSource(OrderSource.CUSTOMER_APP);
        order.setOrderTime(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);
        order.setNote(request.getNote());

        DeliveryMethod method = parseDeliveryMethod(request.getDeliveryMethod());
        if (method == DeliveryMethod.DINE_IN) {
            throw new RuntimeException("Khach tu dat hien chi ho tro DELIVERY hoac TAKEAWAY");
        }
        order.setDeliveryMethod(method);

        setDeliveryAddress(order, method, request, customer);

        double totalAmount = 0;
        for (OrderRequest.CartItem item : request.getItems()) {
            DishVariant variant = dishVariantRepository.findById(item.getVariantId())
                    .orElseThrow(() -> new RuntimeException("Món/Size ID " + item.getVariantId() + " không tồn tại"));

            if (!variant.getDish().isAvailable()) {
                throw new RuntimeException("Món " + variant.getDish().getName() + " hiện đã ngừng phục vụ");
            }

            boolean hasToppings = item.getToppingIds() != null && !item.getToppingIds().isEmpty();
            String categoryName = variant.getDish().getCategory().getName();
            if (hasToppings && !categoryName.equalsIgnoreCase("Pizza")) {
                throw new RuntimeException("Chỉ có thể thêm topping cho món Pizza");
            }

            OrderDetail detail = new OrderDetail();
            detail.setDishVariant(variant);
            detail.setQuantity(item.getQuantity());

            double toppingsPrice = 0;
            List<Topping> toppings = new ArrayList<>();
            if (hasToppings) {
                toppings = toppingRepository.findAllById(item.getToppingIds());
                detail.setToppings(toppings);
                for (Topping topping : toppings) {
                    toppingsPrice += topping.getPrice();
                }
            }

            double finalUnitPrice = variant.getPrice() + toppingsPrice;
            detail.setUnitPrice(finalUnitPrice);
            double subTotal = finalUnitPrice * item.getQuantity();
            detail.setSubTotal(subTotal);

            totalAmount += subTotal;
            order.addDetail(detail);
        }

        if (method == DeliveryMethod.TAKEAWAY) {
            if (request.getBranchId() == null) {
                throw new RuntimeException("Vui long chon chi nhanh nhan don TAKEAWAY");
            }
            assignSpecificBranchAndDeductStock(order, request.getBranchId());
        } else {
            assignBestBranchAndDeductStock(order, request.getCustomerLat(), request.getCustomerLng());
        }

        order.setTotalPrice(totalAmount);
        double discountAmount = applyCouponIfNeeded(order, customer, request, totalAmount);
        order.setFinalTotalPrice(totalAmount - discountAmount);

        Order savedOrder = orderRepository.save(order);
        orderRealtimeService.publishOrderCreated(savedOrder);
        menuAvailabilityRealtimeService.publishChanged(savedOrder.getBranch());
        return savedOrder;
    }

    @Transactional
    public Order createStaffOrder(String staffUsername, StaffOrderRequest request) {
        validateOrderRequest(request);

        DeliveryMethod method = parseDeliveryMethod(request.getDeliveryMethod());
        Customer customer = method == DeliveryMethod.DELIVERY
                ? resolveCustomerForStaffOrder(request)
                : resolveOptionalCustomerForStaffOrder(request);
        Account staffAccount = branchAccessService.getAccount(staffUsername);
        Employee staff = staffAccount.getRole() == RoleName.SUPER_ADMIN
                ? employeeRepository.findByAccount_Username(staffUsername).orElse(null)
                : branchAccessService.getEmployee(staffUsername);
        Long visibleBranchId = branchAccessService.resolveVisibleBranchId(staffUsername, request.getBranchId());
        if (visibleBranchId == null) {
            throw new RuntimeException("Vui long chon chi nhanh tao don");
        }
        Branch orderBranch = branchRepository.findById(visibleBranchId)
                .orElseThrow(() -> new RuntimeException("Chi nhanh khong ton tai"));
        if (!orderBranch.isActive()) {
            throw new RuntimeException("Chi nhanh dang bi khoa");
        }

        Order order = new Order();
        order.setCustomer(customer);
        order.setOrderSource(OrderSource.STAFF_COUNTER);
        order.setOrderTime(LocalDateTime.now());
        order.setStatus(OrderStatus.CONFIRMED);
        if (staff != null) {
            order.setHandledBy(staff);
        }
        order.setAcceptedAt(LocalDateTime.now());
        order.setNote(request.getNote());

        order.setDeliveryMethod(method);

        setDeliveryAddress(order, method, request, customer);

        double totalAmount = 0;
        for (OrderRequest.CartItem item : request.getItems()) {
            DishVariant variant = dishVariantRepository.findById(item.getVariantId())
                    .orElseThrow(() -> new RuntimeException("Mon/Size ID " + item.getVariantId() + " khong ton tai"));

            if (!variant.getDish().isAvailable()) {
                throw new RuntimeException("Mon " + variant.getDish().getName() + " hien da ngung phuc vu");
            }

            boolean hasToppings = item.getToppingIds() != null && !item.getToppingIds().isEmpty();
            String categoryName = variant.getDish().getCategory().getName();
            if (hasToppings && !categoryName.equalsIgnoreCase("Pizza")) {
                throw new RuntimeException("Chi co the them topping cho mon Pizza");
            }

            OrderDetail detail = new OrderDetail();
            detail.setDishVariant(variant);
            detail.setQuantity(item.getQuantity());

            double toppingsPrice = 0;
            List<Topping> toppings = new ArrayList<>();
            if (hasToppings) {
                toppings = toppingRepository.findAllById(item.getToppingIds());
                detail.setToppings(toppings);
                for (Topping topping : toppings) {
                    toppingsPrice += topping.getPrice();
                }
            }

            double finalUnitPrice = variant.getPrice() + toppingsPrice;
            detail.setUnitPrice(finalUnitPrice);
            double subTotal = finalUnitPrice * item.getQuantity();
            detail.setSubTotal(subTotal);

            totalAmount += subTotal;
            order.addDetail(detail);
        }

        assignSpecificBranchAndDeductStock(order, orderBranch.getId());

        order.setTotalPrice(totalAmount);
        double discountAmount = applyCouponIfNeeded(order, customer, request, totalAmount);
        order.setFinalTotalPrice(totalAmount - discountAmount);

        Order savedOrder = orderRepository.save(order);
        orderRealtimeService.publishOrderCreated(savedOrder);
        menuAvailabilityRealtimeService.publishChanged(savedOrder.getBranch());
        return savedOrder;
    }

    private Customer resolveCustomerForStaffOrder(StaffOrderRequest request) {
        if (request.getCustomerId() != null) {
            return customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new RuntimeException("Khong tim thay khach hang"));
        }

        String phoneNumber = trimToNull(request.getCustomerPhoneNumber());
        if (phoneNumber == null) {
            throw new RuntimeException("Vui long chon khach hang hoac nhap so dien thoai khach hang");
        }

        return customerRepository.findByPhoneNumber(phoneNumber)
                .orElseGet(() -> createCustomerFromStaffOrder(request, phoneNumber));
    }

    private Customer resolveOptionalCustomerForStaffOrder(StaffOrderRequest request) {
        if (request.getCustomerId() == null && trimToNull(request.getCustomerPhoneNumber()) == null) {
            return null;
        }
        return resolveCustomerForStaffOrder(request);
    }

    private Customer createCustomerFromStaffOrder(StaffOrderRequest request, String phoneNumber) {
        if (accountRepository.existsByUsername(phoneNumber)) {
            throw new RuntimeException("So dien thoai da ton tai nhung chua gan voi ho so khach hang");
        }

        String fullName = trimToNull(request.getCustomerFullName());
        if (fullName == null) {
            throw new RuntimeException("Vui long nhap ho ten khach hang moi");
        }

        String email = trimToNull(request.getCustomerEmail());
        if (email != null && customerRepository.existsByEmail(email)) {
            throw new RuntimeException("Email nay da duoc su dung cho khach hang khac");
        }

        Account account = new Account();
        account.setUsername(phoneNumber);
        account.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        account.setRole(RoleName.CUSTOMER);
        account.setFirstLogin(true);

        Customer customer = new Customer();
        customer.setFullName(fullName);
        customer.setPhoneNumber(phoneNumber);
        customer.setEmail(email);
        customer.setAddress(trimToNull(request.getDeliveryAddress()));
        customer.setAccount(account);
        return customerRepository.save(customer);
    }

    private void validateOrderRequest(OrderRequest request) {
        if (request == null) {
            throw new RuntimeException("Thong tin don hang khong duoc de trong");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("Don hang phai co it nhat mot mon");
        }
        for (OrderRequest.CartItem item : request.getItems()) {
            if (item.getVariantId() == null) {
                throw new RuntimeException("Vui long chon bien the mon");
            }
            if (item.getQuantity() <= 0) {
                throw new RuntimeException("So luong mon phai lon hon 0");
            }
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private DeliveryMethod parseDeliveryMethod(String value) {
        String method = trimToNull(value);
        if (method == null) {
            return DeliveryMethod.DELIVERY;
        }
        try {
            return DeliveryMethod.valueOf(method.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            throw new RuntimeException("Hinh thuc nhan mon khong hop le");
        }
    }

    private void setDeliveryAddress(Order order, DeliveryMethod method, OrderRequest request, Customer customer) {
        if (method != DeliveryMethod.DELIVERY) {
            order.setDeliveryAddress(null);
            return;
        }

        String requestedAddress = trimToNull(request.getDeliveryAddress());
        if (requestedAddress != null) {
            order.setDeliveryAddress(requestedAddress);
            return;
        }

        String customerAddress = customer == null ? null : trimToNull(customer.getAddress());
        if (customerAddress == null) {
            throw new RuntimeException("Vui long nhap dia chi giao hang");
        }
        order.setDeliveryAddress(customerAddress);
    }

    public List<Order> getInternalOrders(String username, Long branchId) {
        Long visibleBranchId = branchAccessService.resolveVisibleBranchId(username, branchId);
        if (visibleBranchId == null) {
            return orderRepository.findAll();
        }
        return orderRepository.findByBranch_IdOrderByOrderTimeDesc(visibleBranchId);
    }

    @Transactional
    public void approveOrder(Long orderId, String staffUsername) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));

        branchAccessService.assertCanAccessOrder(staffUsername, order);
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PAID) {
            throw new RuntimeException("Đơn hàng này không thể duyệt");
        }

        OrderStatus previousStatus = order.getStatus();

        Employee staff = employeeRepository.findByAccount_Username(staffUsername)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin nhân viên: " + staffUsername));
        order.setHandledBy(staff);
        order.setAcceptedAt(LocalDateTime.now());
        order.setStatus(OrderStatus.CONFIRMED);
        Order savedOrder = orderRepository.save(order);
        orderRealtimeService.publishOrderConfirmed(savedOrder, previousStatus);
    }

    @Transactional
    public void startCooking(Long orderId, String chefUsername) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Don hang khong ton tai"));

        branchAccessService.assertCanAccessOrder(chefUsername, order);
        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new RuntimeException("Chi co the bat dau nau don da duoc xac nhan");
        }

        OrderStatus previousStatus = order.getStatus();
        Employee chef = employeeRepository.findByAccount_Username(chefUsername)
                .orElseThrow(() -> new RuntimeException("Khong tim thay thong tin dau bep: " + chefUsername));

        order.setCookedBy(chef);
        order.setCookingStartedAt(LocalDateTime.now());
        order.setStatus(OrderStatus.COOKING);
        Order savedOrder = orderRepository.save(order);
        orderRealtimeService.publishOrderStatusChanged(savedOrder, previousStatus);
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus) {
        return updateOrderStatus(orderId, newStatus, null);
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus newStatus, String username) {
        if (newStatus == OrderStatus.CANCELLED) {
            throw new RuntimeException("Để hủy đơn, vui lòng dùng API cancel");
        }
        if (newStatus == OrderStatus.COOKING) {
            throw new RuntimeException("De chuyen sang COOKING, vui long dung API start-cooking");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));

        if (username != null) {
            branchAccessService.assertCanAccessOrder(username, order);
        }
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.COMPLETED) {
            throw new RuntimeException("Không thể cập nhật trạng thái cho đơn hàng này");
        }

        if (order.getStatus() == newStatus) {
            throw new RuntimeException("Đơn hàng đã ở trạng thái " + newStatus);
        }

        OrderStatus previousStatus = order.getStatus();
        order.setStatus(newStatus);
        if (newStatus == OrderStatus.COMPLETED && order.getCompletedAt() == null) {
            order.setCompletedAt(LocalDateTime.now());
        }
        Order savedOrder = orderRepository.save(order);
        orderRealtimeService.publishOrderStatusChanged(savedOrder, previousStatus);
        return savedOrder;
    }

    @Transactional
    public void cancelOrder(Long orderId, String reason) {
        cancelOrder(orderId, reason, null);
    }

    @Transactional
    public void cancelOrder(Long orderId, String reason, String username) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));

        if (username != null) {
            assertCanCancelOrder(username, order);
        }
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.COMPLETED) {
            throw new RuntimeException("Không thể hủy đơn hàng này");
        }

        OrderStatus previousStatus = order.getStatus();

        List<InventoryBatchConsumption> consumptions = inventoryBatchConsumptionRepository.findByOrderDetail_Order_IdAndReturnedFalse(orderId);
        if (!consumptions.isEmpty()) {
            restoreBatchConsumptions(consumptions);
        } else {
            restoreLegacyInventory(order);
        }

        if (order.getCouponCode() != null) {
            couponRepository.findByCode(order.getCouponCode()).ifPresent(coupon -> {
                coupon.setUsageCount(coupon.getUsageCount() - 1);
                couponRepository.save(coupon);
            });
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setNote(order.getNote() + " | Da huy: " + reason);
        Order savedOrder = orderRepository.save(order);
        orderRealtimeService.publishOrderCancelled(savedOrder, previousStatus);
        menuAvailabilityRealtimeService.publishChanged(savedOrder.getBranch());
    }

    private void assertCanCancelOrder(String username, Order order) {
        Account account = branchAccessService.getAccount(username);
        if (account.getRole() == RoleName.CUSTOMER) {
            if (order.getCustomer() == null
                    || order.getCustomer().getAccount() == null
                    || !username.equals(order.getCustomer().getAccount().getUsername())) {
                throw new RuntimeException("Khach hang chi duoc huy don cua chinh minh");
            }
            return;
        }

        branchAccessService.assertCanAccessOrder(username, order);
    }

    private void deductStockByBatch(Branch branch, Product product, double totalNeeded, OrderDetail detail) {
        Inventory inventory = inventoryRepository.findByBranchAndProduct(branch, product)
                .orElseThrow(() -> new RuntimeException("Chi nhánh " + branch.getName() + " chưa nhập nguyên liệu: " + product.getName()));

        if (inventory.getQuantityAvailable() < totalNeeded) {
            throw new RuntimeException("HẾT HÀNG! Chi nhánh " + branch.getName() + " không đủ nguyên liệu: " + product.getName());
        }

        double remainingNeeded = totalNeeded;
        List<InventoryBatch> usableBatches = inventoryBatchRepository.findUsableBatchesForDeduction(branch, product, LocalDate.now());
        for (InventoryBatch batch : usableBatches) {
            if (remainingNeeded <= 0) {
                break;
            }

            double deducted = Math.min(batch.getQuantityRemaining(), remainingNeeded);
            batch.setQuantityRemaining(batch.getQuantityRemaining() - deducted);
            inventoryBatchRepository.save(batch);
            detail.addBatchConsumption(new InventoryBatchConsumption(detail, batch, deducted));
            remainingNeeded -= deducted;
        }

        if (remainingNeeded > 0) {
            throw new RuntimeException("HẾT HÀNG! Chi nhánh " + branch.getName()
                    + " không đủ lô nguyên liệu còn hạn cho: " + product.getName());
        }

        inventory.setQuantityAvailable(inventory.getQuantityAvailable() - totalNeeded);
        inventoryRepository.save(inventory);
    }

    private void restoreBatchConsumptions(List<InventoryBatchConsumption> consumptions) {
        for (InventoryBatchConsumption consumption : consumptions) {
            InventoryBatch batch = consumption.getBatch();
            batch.setQuantityRemaining(batch.getQuantityRemaining() + consumption.getQuantity());
            inventoryBatchRepository.save(batch);

            inventoryRepository.findByBranchAndProduct(batch.getBranch(), batch.getProduct()).ifPresent(inventory -> {
                inventory.setQuantityAvailable(inventory.getQuantityAvailable() + consumption.getQuantity());
                inventoryRepository.save(inventory);
            });

            consumption.setReturned(true);
            consumption.setReturnedAt(LocalDateTime.now());
            inventoryBatchConsumptionRepository.save(consumption);
        }
    }

    private void restoreLegacyInventory(Order order) {
        Branch orderBranch = order.getBranch();
        for (OrderDetail detail : order.getOrderDetails()) {
            int qty = detail.getQuantity();
            DishVariant variant = detail.getDishVariant();

            for (Recipe recipe : variant.getRecipes()) {
                Product product = recipe.getProduct();
                double amountToReturn = recipe.getQuantityNeeded() * qty;
                restoreInventoryTotal(orderBranch, product, amountToReturn);
            }

            if (detail.getToppings() != null) {
                for (Topping topping : detail.getToppings()) {
                    Product product = topping.getProduct();
                    double amountToReturn = topping.getQuantityNeeded() * qty;
                    restoreInventoryTotal(orderBranch, product, amountToReturn);
                }
            }
        }
    }

    private void restoreInventoryTotal(Branch branch, Product product, double amountToReturn) {
        inventoryRepository.findByBranchAndProduct(branch, product).ifPresent(inventory -> {
            inventory.setQuantityAvailable(inventory.getQuantityAvailable() + amountToReturn);
            inventoryRepository.save(inventory);
        });
    }

    private double applyCouponIfNeeded(Order order, Customer customer, OrderRequest request, double totalAmount) {
        double discountAmount = 0;
        if (request.getCouponCode() == null || request.getCouponCode().trim().isEmpty()) {
            return discountAmount;
        }
        if (customer == null) {
            throw new RuntimeException("Khach vang lai khong the dung ma giam gia");
        }

        String code = request.getCouponCode().toUpperCase();
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Mã giảm giá không tồn tại"));

        if (!coupon.isActive()) {
            throw new RuntimeException("Mã giảm giá đang bị khóa");
        }
        if (coupon.getExpirationDate() != null && LocalDate.now().isAfter(coupon.getExpirationDate())) {
            throw new RuntimeException("Mã giảm giá đã hết hạn");
        }
        if (coupon.getUsageLimit() != null && coupon.getUsageCount() >= coupon.getUsageLimit()) {
            throw new RuntimeException("Mã giảm giá đã hết lượt sử dụng");
        }
        if (coupon.getMinOrderAmount() != null && totalAmount < coupon.getMinOrderAmount()) {
            throw new RuntimeException("Đơn hàng chưa đủ giá trị tối thiểu");
        }
        long usedCount = orderRepository.countUsedByCustomer(customer.getId(), code);
        if (usedCount > 0) {
            throw new RuntimeException("Bạn đã dùng mã này rồi");
        }

        if (coupon.getDiscountPercent() != null && coupon.getDiscountPercent() > 0) {
            discountAmount = totalAmount * (coupon.getDiscountPercent() / 100.0);
            if (coupon.getMaxDiscountAmount() != null && discountAmount > coupon.getMaxDiscountAmount()) {
                discountAmount = coupon.getMaxDiscountAmount();
            }
        } else if (coupon.getDiscountAmount() != null) {
            discountAmount = coupon.getDiscountAmount();
        }
        if (discountAmount > totalAmount) {
            discountAmount = totalAmount;
        }

        coupon.setUsageCount(coupon.getUsageCount() + 1);
        couponRepository.save(coupon);
        order.setCouponCode(code);
        order.setDiscountAmount(discountAmount);
        return discountAmount;
    }

    private void assignBestBranchAndDeductStock(Order order, Double customerLat, Double customerLng) {
        List<BranchCandidate> candidates = buildBranchCandidates(customerLat, customerLng);
        if (candidates.isEmpty()) {
            throw new RuntimeException("Không có chi nhánh đang hoạt động để xử lý đơn hàng");
        }

        for (BranchCandidate candidate : candidates) {
            if (tryAssignBranchAndDeductStock(order, candidate.branchId())) {
                return;
            }
        }

        throw new RuntimeException("Không có chi nhánh nào đủ khả năng nhận đơn lúc này");
    }

    private void assignSpecificBranchAndDeductStock(Order order, Long branchId) {
        if (!tryAssignBranchAndDeductStock(order, branchId)) {
            throw new RuntimeException("Chi nhánh đã chọn đang quá tải hoặc không đủ nguyên liệu");
        }
    }

    private boolean tryAssignBranchAndDeductStock(Order order, Long branchId) {
        Branch lockedBranch = branchRepository.findByIdForUpdate(branchId)
                .orElseThrow(() -> new RuntimeException("Chi nhánh không tồn tại"));
        if (!lockedBranch.isActive()) {
            return false;
        }

        long currentLoad = countCurrentLoad(lockedBranch);
        if (currentLoad >= getMaxPendingCookOrders(lockedBranch)) {
            return false;
        }

        Map<Product, Double> requiredProducts = buildRequiredProducts(order.getOrderDetails());
        if (requiredProducts.isEmpty()) {
            order.setBranch(lockedBranch);
            return true;
        }

        List<Product> products = requiredProducts.keySet().stream()
                .sorted(Comparator.comparing(Product::getId))
                .toList();
        List<Inventory> lockedInventories = inventoryRepository.findByBranchAndProductsForUpdate(lockedBranch, products);
        List<InventoryBatch> lockedBatches = inventoryBatchRepository.findUsableBatchesForUpdate(
                lockedBranch,
                products,
                LocalDate.now()
        );

        if (!hasEnoughStock(requiredProducts, lockedInventories, lockedBatches)) {
            return false;
        }

        deductLockedStock(lockedBranch, order.getOrderDetails(), lockedInventories, lockedBatches);
        order.setBranch(lockedBranch);
        return true;
    }

    private List<BranchCandidate> buildBranchCandidates(Double customerLat, Double customerLng) {
        return branchRepository.findAll().stream()
                .filter(Branch::isActive)
                .map(branch -> toBranchCandidate(branch, customerLat, customerLng))
                .sorted(Comparator
                        .comparing(BranchCandidate::outsideRadius)
                        .thenComparingDouble(BranchCandidate::score)
                        .thenComparingDouble(BranchCandidate::distanceKm)
                        .thenComparing(BranchCandidate::branchId))
                .toList();
    }

    private BranchCandidate toBranchCandidate(Branch branch, Double customerLat, Double customerLng) {
        double distanceKm = calculateDistanceKm(branch, customerLat, customerLng);
        boolean outsideRadius = distanceKm > getMaxServiceRadiusKm(branch);
        long currentLoad = orderRepository.countByBranchIdAndStatusIn(branch.getId(), PENDING_COOK_SLOT_STATUSES);
        double loadRatio = currentLoad / (double) getMaxPendingCookOrders(branch);
        double score = (distanceKm * 0.7) + (loadRatio * 10.0 * 0.3);
        return new BranchCandidate(branch.getId(), distanceKm, outsideRadius, score);
    }

    private double calculateDistanceKm(Branch branch, Double customerLat, Double customerLng) {
        if (customerLat == null || customerLng == null || branch.getLatitude() == null || branch.getLongitude() == null) {
            return Double.MAX_VALUE;
        }
        return LocationUtils.calculateDistance(customerLat, customerLng, branch.getLatitude(), branch.getLongitude());
    }

    private long countCurrentLoad(Branch branch) {
        return orderRepository.countByBranchIdAndStatusIn(branch.getId(), PENDING_COOK_SLOT_STATUSES);
    }

    private int getMaxPendingCookOrders(Branch branch) {
        return branch.getMaxPendingCookOrders() == null || branch.getMaxPendingCookOrders() <= 0
                ? DEFAULT_MAX_PENDING_COOK_ORDERS
                : branch.getMaxPendingCookOrders();
    }

    private double getMaxServiceRadiusKm(Branch branch) {
        return branch.getMaxServiceRadiusKm() == null || branch.getMaxServiceRadiusKm() <= 0
                ? DEFAULT_MAX_SERVICE_RADIUS_KM
                : branch.getMaxServiceRadiusKm();
    }

    private boolean hasEnoughStock(Map<Product, Double> requiredProducts,
                                   List<Inventory> inventories,
                                   List<InventoryBatch> batches) {
        Map<Long, Double> inventoryByProductId = new HashMap<>();
        for (Inventory inventory : inventories) {
            inventoryByProductId.put(inventory.getProduct().getId(), inventory.getQuantityAvailable());
        }

        Map<Long, Double> batchQuantityByProductId = new HashMap<>();
        for (InventoryBatch batch : batches) {
            batchQuantityByProductId.merge(batch.getProduct().getId(), batch.getQuantityRemaining(), Double::sum);
        }

        for (Map.Entry<Product, Double> entry : requiredProducts.entrySet()) {
            Long productId = entry.getKey().getId();
            double requiredQuantity = entry.getValue();
            double inventoryQuantity = inventoryByProductId.getOrDefault(productId, 0.0);
            double batchQuantity = batchQuantityByProductId.getOrDefault(productId, 0.0);
            if (Math.min(inventoryQuantity, batchQuantity) < requiredQuantity) {
                return false;
            }
        }
        return true;
    }

    private void deductLockedStock(Branch branch,
                                   List<OrderDetail> details,
                                   List<Inventory> inventories,
                                   List<InventoryBatch> batches) {
        Map<Long, Inventory> inventoryByProductId = new HashMap<>();
        for (Inventory inventory : inventories) {
            inventoryByProductId.put(inventory.getProduct().getId(), inventory);
        }

        Map<Long, List<InventoryBatch>> batchesByProductId = new HashMap<>();
        for (InventoryBatch batch : batches) {
            batchesByProductId.computeIfAbsent(batch.getProduct().getId(), ignored -> new ArrayList<>()).add(batch);
        }

        for (OrderDetail detail : details) {
            DishVariant variant = detail.getDishVariant();
            int quantityOrdered = detail.getQuantity();

            for (Recipe recipe : variant.getRecipes()) {
                deductLockedProduct(branch, recipe.getProduct(), recipe.getQuantityNeeded() * quantityOrdered,
                        detail, inventoryByProductId, batchesByProductId);
            }

            if (detail.getToppings() != null) {
                for (Topping topping : detail.getToppings()) {
                    deductLockedProduct(branch, topping.getProduct(), topping.getQuantityNeeded() * quantityOrdered,
                            detail, inventoryByProductId, batchesByProductId);
                }
            }
        }
    }

    private void deductLockedProduct(Branch branch,
                                     Product product,
                                     double totalNeeded,
                                     OrderDetail detail,
                                     Map<Long, Inventory> inventoryByProductId,
                                     Map<Long, List<InventoryBatch>> batchesByProductId) {
        Inventory inventory = inventoryByProductId.get(product.getId());
        if (inventory == null || inventory.getQuantityAvailable() < totalNeeded) {
            throw new RuntimeException("HẾT HÀNG! Chi nhánh " + branch.getName() + " không đủ nguyên liệu: " + product.getName());
        }

        double remainingNeeded = totalNeeded;
        List<InventoryBatch> productBatches = batchesByProductId.getOrDefault(product.getId(), List.of());
        for (InventoryBatch batch : productBatches) {
            if (remainingNeeded <= 0) {
                break;
            }

            double deducted = Math.min(batch.getQuantityRemaining(), remainingNeeded);
            batch.setQuantityRemaining(batch.getQuantityRemaining() - deducted);
            inventoryBatchRepository.save(batch);
            detail.addBatchConsumption(new InventoryBatchConsumption(detail, batch, deducted));
            remainingNeeded -= deducted;
        }

        if (remainingNeeded > 0) {
            throw new RuntimeException("HẾT HÀNG! Chi nhánh " + branch.getName()
                    + " không đủ lô nguyên liệu còn hạn cho: " + product.getName());
        }

        inventory.setQuantityAvailable(inventory.getQuantityAvailable() - totalNeeded);
        inventoryRepository.save(inventory);
    }

    private record BranchCandidate(Long branchId, double distanceKm, boolean outsideRadius, double score) {
    }

    private Branch findBestBranchForOrder(List<OrderDetail> details, Double customerLat, Double customerLng) {
        List<Branch> bestBranches = new ArrayList<>();
        int bestCapacity = 0;

        for (Branch branch : branchRepository.findAll()) {
            if (!branch.isActive()) {
                continue;
            }

            int capacity = calculateOrderCapacity(branch, details);
            if (capacity <= 0) {
                continue;
            }

            if (capacity > bestCapacity) {
                bestCapacity = capacity;
                bestBranches.clear();
                bestBranches.add(branch);
            } else if (capacity == bestCapacity) {
                bestBranches.add(branch);
            }
        }

        if (bestBranches.isEmpty()) {
            throw new RuntimeException("HẾT HÀNG! Không có chi nhánh nào đủ nguyên liệu để xử lý đơn hàng này");
        }

        return findNearestBranch(customerLat, customerLng, bestBranches);
    }

    private int calculateOrderCapacity(Branch branch, List<OrderDetail> details) {
        Map<Product, Double> requiredProducts = buildRequiredProducts(details);
        if (requiredProducts.isEmpty()) {
            return Integer.MAX_VALUE;
        }

        int maxCanFulfill = Integer.MAX_VALUE;
        for (Map.Entry<Product, Double> entry : requiredProducts.entrySet()) {
            double requiredQuantity = entry.getValue();
            if (requiredQuantity <= 0) {
                continue;
            }

            Inventory inventory = inventoryRepository.findByBranchAndProduct(branch, entry.getKey()).orElse(null);
            if (inventory == null) {
                return 0;
            }

            double usableBatchQuantity = inventoryBatchRepository
                    .findUsableBatchesForDeduction(branch, entry.getKey(), LocalDate.now())
                    .stream()
                    .mapToDouble(InventoryBatch::getQuantityRemaining)
                    .sum();

            double usableQuantity = Math.min(inventory.getQuantityAvailable(), usableBatchQuantity);
            int possible = (int) (usableQuantity / requiredQuantity);
            if (possible < maxCanFulfill) {
                maxCanFulfill = possible;
            }
        }

        return maxCanFulfill == Integer.MAX_VALUE ? 0 : maxCanFulfill;
    }

    private Map<Product, Double> buildRequiredProducts(List<OrderDetail> details) {
        Map<Product, Double> requiredProducts = new HashMap<>();

        for (OrderDetail detail : details) {
            int quantityOrdered = detail.getQuantity();
            DishVariant variant = detail.getDishVariant();

            for (Recipe recipe : variant.getRecipes()) {
                Product product = recipe.getProduct();
                double totalNeeded = recipe.getQuantityNeeded() * quantityOrdered;
                requiredProducts.merge(product, totalNeeded, Double::sum);
            }

            if (detail.getToppings() != null) {
                for (Topping topping : detail.getToppings()) {
                    Product product = topping.getProduct();
                    double totalNeeded = topping.getQuantityNeeded() * quantityOrdered;
                    requiredProducts.merge(product, totalNeeded, Double::sum);
                }
            }
        }

        return requiredProducts;
    }

    public Branch findNearestBranch(Double customerLat, Double customerLng) {
        List<Branch> branches = branchRepository.findAll().stream()
                .filter(Branch::isActive)
                .toList();
        return findNearestBranch(customerLat, customerLng, branches);
    }

    private Branch findNearestBranch(Double customerLat, Double customerLng, List<Branch> branches) {
        if (customerLat == null || customerLng == null || branches.isEmpty()) {
            return branches.isEmpty() ? null : branches.get(0);
        }

        StringBuilder destinations = new StringBuilder();
        for (int i = 0; i < branches.size(); i++) {
            Branch branch = branches.get(i);
            destinations.append(branch.getLatitude()).append(",").append(branch.getLongitude());
            if (i < branches.size() - 1) {
                destinations.append("|");
            }
        }

        String url = String.format(
                "https://rsapi.goong.io/DistanceMatrix?origins=%s,%s&destinations=%s&vehicle=bike&api_key=%s",
                customerLat, customerLng, destinations, goongApiKey
        );

        try {
            RestTemplate restTemplate = new RestTemplate();
            com.pizzastore.dto.GoongDistanceResponse response = restTemplate.getForObject(url, com.pizzastore.dto.GoongDistanceResponse.class);

            if (response != null && response.getRows() != null && !response.getRows().isEmpty()) {
                List<com.pizzastore.dto.GoongDistanceResponse.Element> elements = response.getRows().get(0).getElements();
                Branch fastestBranch = null;
                int minDuration = Integer.MAX_VALUE;

                for (int i = 0; i < elements.size(); i++) {
                    com.pizzastore.dto.GoongDistanceResponse.Element element = elements.get(i);
                    if ("OK".equals(element.getStatus()) && element.getDuration().getValue() < minDuration) {
                        minDuration = element.getDuration().getValue();
                        fastestBranch = branches.get(i);
                    }
                }

                if (fastestBranch != null) {
                    return fastestBranch;
                }
            }
        } catch (Exception e) {
            System.err.println("Loi khi goi Goong API, fallback ve chi nhanh mac dinh: " + e.getMessage());
        }

        return branches.get(0);
    }
}
