package com.pizzastore.service;

import com.pizzastore.dto.OrderRequest;
import com.pizzastore.entity.*;
import com.pizzastore.enums.DeliveryMethod;
import com.pizzastore.enums.OrderStatus;
import com.pizzastore.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final DishVariantRepository dishVariantRepository;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final ProductRepository productRepository;
    private final DishService dishService;
    private final ToppingRepository toppingRepository;
    private final CouponRepository couponRepository;
    private final EmployeeRepository employeeRepository;

    @Autowired
    public OrderService(OrderRepository orderRepository,
                        DishVariantRepository dishVariantRepository,
                        CustomerRepository customerRepository,
                        AccountRepository accountRepository,
                        ProductRepository productRepository,
                        DishService dishService,
                        ToppingRepository toppingRepository,
                        CouponRepository couponRepository,
                        EmployeeRepository employeeRepository) {
        this.orderRepository = orderRepository;
        this.dishVariantRepository = dishVariantRepository;
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.productRepository = productRepository;
        this.dishService = dishService;
        this.toppingRepository = toppingRepository;
        this.couponRepository = couponRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional
    public Order createOrder(String username, OrderRequest request) {
        // 1. Tìm Customer...
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

        // --- XỬ LÝ GIAO HÀNG ---
        DeliveryMethod method = DeliveryMethod.DELIVERY;
        if (request.getDeliveryMethod() != null && !request.getDeliveryMethod().isEmpty()) {
            try {
                method = DeliveryMethod.valueOf(request.getDeliveryMethod().toUpperCase());
            } catch (IllegalArgumentException e) {
                method = DeliveryMethod.DELIVERY;
            }
        }
        order.setDeliveryMethod(method);

        if (method == DeliveryMethod.DELIVERY) {
            if (request.getDeliveryAddress() != null && !request.getDeliveryAddress().trim().isEmpty()) {
                order.setDeliveryAddress(request.getDeliveryAddress());
            } else {
                if (customer.getAddress() == null || customer.getAddress().trim().isEmpty()) {
                    throw new RuntimeException("Vui lòng nhập địa chỉ hoặc cập nhật hồ sơ để giao hàng!");
                }
                order.setDeliveryAddress(customer.getAddress());
            }
        } else {
            order.setDeliveryAddress("Nhận tại quán");
        }

        double totalAmount = 0;

        // 3. Duyệt Items để tính tiền & tạo Detail
        for (OrderRequest.CartItem item : request.getItems()) {
            DishVariant variant = dishVariantRepository.findById(item.getVariantId())
                    .orElseThrow(() -> new RuntimeException("Món/Size ID " + item.getVariantId() + " không tồn tại"));

            if (!variant.getDish().isAvailable()) {
                throw new RuntimeException("Món " + variant.getDish().getName() + " hiện đã ngưng phục vụ!");
            }

            boolean hasToppings = item.getToppingIds() != null && !item.getToppingIds().isEmpty();
            String categoryName = variant.getDish().getCategory().getName();
            if (hasToppings && !categoryName.equalsIgnoreCase("Pizza")) {
                throw new RuntimeException("Chỉ có thể thêm Topping cho món Pizza.");
            }

            OrderDetail detail = new OrderDetail();
            detail.setDishVariant(variant);
            detail.setQuantity(item.getQuantity());

            double toppingsPrice = 0;
            List<Topping> toppings = new ArrayList<>();
            if (hasToppings) {
                toppings = toppingRepository.findAllById(item.getToppingIds());
                detail.setToppings(toppings);
                for (Topping t : toppings) toppingsPrice += t.getPrice();
            }

            double finalUnitPrice = variant.getPrice() + toppingsPrice;
            detail.setUnitPrice(finalUnitPrice);
            double subTotal = finalUnitPrice * item.getQuantity();
            detail.setSubTotal(subTotal);

            totalAmount += subTotal;
            order.addDetail(detail);
        }

        // =========================================================================
        // [QUAN TRỌNG] 4. TRỪ KHO NGAY LẬP TỨC (HARD RESERVATION)
        // Logic này chuyển từ approveOrder LÊN ĐÂY.
        // =========================================================================
        for (OrderDetail detail : order.getOrderDetails()) {
            DishVariant variant = detail.getDishVariant();
            int quantityOrdered = detail.getQuantity();

            // A. Trừ kho theo công thức món chính
            for (Recipe recipe : variant.getRecipes()) {
                Product product = recipe.getProduct();
                double totalNeeded = recipe.getQuantityNeeded() * quantityOrdered;

                if (product.getStockQuantity() < totalNeeded) {
                    throw new RuntimeException("HẾT HÀNG! Món '" + variant.getDish().getName() +
                            "' (" + variant.getSize() + ") không đủ nguyên liệu: " + product.getName());
                }
                product.setStockQuantity(product.getStockQuantity() - totalNeeded);
                productRepository.save(product);
            }

            // B. Trừ kho theo Topping
            if (detail.getToppings() != null) {
                for (Topping topping : detail.getToppings()) {
                    Product product = topping.getProduct();
                    double totalNeeded = topping.getQuantityNeeded() * quantityOrdered;

                    if (product.getStockQuantity() < totalNeeded) {
                        throw new RuntimeException("HẾT HÀNG! Topping '" + topping.getName() +
                                "' không đủ nguyên liệu: " + product.getName());
                    }
                    product.setStockQuantity(product.getStockQuantity() - totalNeeded);
                    productRepository.save(product);
                }
            }
            // Check lại món còn available không
            dishService.refreshDishAvailability(variant.getDish());
        }
        // =========================================================================

        // 5. Tính toán Coupon
        order.setTotalPrice(totalAmount);
        double discountAmount = 0;

        if (request.getCouponCode() != null && !request.getCouponCode().trim().isEmpty()) {
            String code = request.getCouponCode().toUpperCase();
            Coupon coupon = couponRepository.findByCode(code)
                    .orElseThrow(() -> new RuntimeException("Mã giảm giá không tồn tại!"));

            if (!coupon.isActive()) throw new RuntimeException("Mã giảm giá đang bị khóa!");
            if (coupon.getExpirationDate() != null && LocalDate.now().isAfter(coupon.getExpirationDate())) {
                throw new RuntimeException("Mã giảm giá đã hết hạn!");
            }
            if (coupon.getUsageLimit() != null && coupon.getUsageCount() >= coupon.getUsageLimit()) {
                throw new RuntimeException("Mã giảm giá đã hết lượt sử dụng!");
            }
            if (coupon.getMinOrderAmount() != null && totalAmount < coupon.getMinOrderAmount()) {
                throw new RuntimeException("Đơn hàng chưa đủ giá trị tối thiểu!");
            }
            long usedCount = orderRepository.countUsedByCustomer(customer.getId(), code);
            if (usedCount > 0) throw new RuntimeException("Bạn đã dùng mã này rồi!");

            if (coupon.getDiscountPercent() != null && coupon.getDiscountPercent() > 0) {
                discountAmount = totalAmount * (coupon.getDiscountPercent() / 100.0);
                if (coupon.getMaxDiscountAmount() != null && discountAmount > coupon.getMaxDiscountAmount()) {
                    discountAmount = coupon.getMaxDiscountAmount();
                }
            } else if (coupon.getDiscountAmount() != null) {
                discountAmount = coupon.getDiscountAmount();
            }
            if (discountAmount > totalAmount) discountAmount = totalAmount;

            coupon.setUsageCount(coupon.getUsageCount() + 1);
            couponRepository.save(coupon);

            order.setCouponCode(code);
            order.setDiscountAmount(discountAmount);
        }

        order.setFinalTotalPrice(totalAmount - discountAmount);

        // Lưu đơn hàng (Lúc này kho đã bị trừ thành công)
        return orderRepository.save(order);
    }

    // [HÀM ĐÃ SỬA] CHỈ CÒN GÁN NHÂN VIÊN VÀ ĐỔI TRẠNG THÁI
    @Transactional
    public void approveOrder(Long orderId, String staffUsername) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PAID) {
            throw new RuntimeException("Đơn hàng này không thể duyệt (đang nấu hoặc đã hủy)!");
        }

        Employee staff = employeeRepository.findByAccount_Username(staffUsername)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin nhân viên: " + staffUsername));
        order.setHandledBy(staff);

        // [LƯU Ý]: Đã xóa đoạn code trừ kho ở đây đi rồi nhé!

        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
    }

    // [HÀM MỚI] CẦN THÊM HÀM NÀY ĐỂ HOÀN LẠI KHO NẾU HỦY ĐƠN
    @Transactional
    public void cancelOrder(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));

        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.COMPLETED) {
            throw new RuntimeException("Không thể hủy đơn hàng này!");
        }

        // --- HOÀN TRẢ LẠI KHO (ROLLBACK STOCK) ---
        for (OrderDetail detail : order.getOrderDetails()) {
            int qty = detail.getQuantity();
            DishVariant variant = detail.getDishVariant();

            // 1. Hoàn nguyên liệu món chính
            for (Recipe recipe : variant.getRecipes()) {
                Product p = recipe.getProduct();
                p.setStockQuantity(p.getStockQuantity() + (recipe.getQuantityNeeded() * qty));
                productRepository.save(p);
            }

            // 2. Hoàn nguyên liệu topping
            if (detail.getToppings() != null) {
                for (Topping t : detail.getToppings()) {
                    Product p = t.getProduct();
                    p.setStockQuantity(p.getStockQuantity() + (t.getQuantityNeeded() * qty));
                    productRepository.save(p);
                }
            }
            dishService.refreshDishAvailability(variant.getDish());
        }

        // 3. Hoàn lại lượt dùng Coupon (nếu có)
        if (order.getCouponCode() != null) {
            couponRepository.findByCode(order.getCouponCode()).ifPresent(coupon -> {
                coupon.setUsageCount(coupon.getUsageCount() - 1);
                couponRepository.save(coupon);
            });
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setNote(order.getNote() + " | Đã hủy: " + reason);
        orderRepository.save(order);
    }
}