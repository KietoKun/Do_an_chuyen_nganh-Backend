package com.pizzastore.service;

import com.pizzastore.dto.OrderRequest;
import com.pizzastore.entity.*;
import com.pizzastore.enums.DeliveryMethod;
import com.pizzastore.enums.OrderStatus;
import com.pizzastore.enums.RoleName;
import com.pizzastore.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final BranchAccessService branchAccessService;

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
                        BranchAccessService branchAccessService) {
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
        this.branchAccessService = branchAccessService;
    }

    @Transactional
    public Order createOrder(String username, OrderRequest request) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));

        Customer customer = customerRepository.findAll().stream()
                .filter(c -> c.getAccount().getId().equals(account.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin khách hàng"));

        Order order = new Order();
        order.setCustomer(customer);
        order.setOrderTime(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);
        order.setNote(request.getNote());

        DeliveryMethod method = DeliveryMethod.DELIVERY;
        if (request.getDeliveryMethod() != null && !request.getDeliveryMethod().isEmpty()) {
            try {
                method = DeliveryMethod.valueOf(request.getDeliveryMethod().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                method = DeliveryMethod.DELIVERY;
            }
        }
        order.setDeliveryMethod(method);

        if (method == DeliveryMethod.DELIVERY) {
            if (request.getDeliveryAddress() != null && !request.getDeliveryAddress().trim().isEmpty()) {
                order.setDeliveryAddress(request.getDeliveryAddress());
            } else {
                if (customer.getAddress() == null || customer.getAddress().trim().isEmpty()) {
                    throw new RuntimeException("Vui lòng nhập địa chỉ hoặc cập nhật hồ sơ để giao hàng");
                }
                order.setDeliveryAddress(customer.getAddress());
            }
        } else {
            order.setDeliveryAddress("Nhan tai quan");
        }

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

        Branch orderBranch = findBestBranchForOrder(order.getOrderDetails(), request.getCustomerLat(), request.getCustomerLng());
        order.setBranch(orderBranch);
        for (OrderDetail detail : order.getOrderDetails()) {
            DishVariant variant = detail.getDishVariant();
            int quantityOrdered = detail.getQuantity();

            for (Recipe recipe : variant.getRecipes()) {
                Product product = recipe.getProduct();
                double totalNeeded = recipe.getQuantityNeeded() * quantityOrdered;
                deductStockByBatch(orderBranch, product, totalNeeded, detail);
            }

            if (detail.getToppings() != null) {
                for (Topping topping : detail.getToppings()) {
                    Product product = topping.getProduct();
                    double totalNeeded = topping.getQuantityNeeded() * quantityOrdered;
                    deductStockByBatch(orderBranch, product, totalNeeded, detail);
                }
            }
        }

        order.setTotalPrice(totalAmount);
        double discountAmount = applyCouponIfNeeded(order, customer, request, totalAmount);
        order.setFinalTotalPrice(totalAmount - discountAmount);

        Order savedOrder = orderRepository.save(order);
        orderRealtimeService.publishOrderCreated(savedOrder);
        return savedOrder;
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
