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

    // [1] THÊM REPOSITORY NÀY ĐỂ TÌM NHÂN VIÊN
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
                        EmployeeRepository employeeRepository) { // [1] Thêm vào Constructor
        this.orderRepository = orderRepository;
        this.dishVariantRepository = dishVariantRepository;
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.productRepository = productRepository;
        this.dishService = dishService;
        this.toppingRepository = toppingRepository;
        this.couponRepository = couponRepository;
        this.employeeRepository = employeeRepository; // [1] Gán biến
    }

    @Transactional
    public Order createOrder(String username, OrderRequest request) {
        // ... (GIỮ NGUYÊN TOÀN BỘ LOGIC TẠO ĐƠN CỦA BẠN Ở ĐÂY) ...
        // Vì lúc tạo đơn là khách tạo, chưa có nhân viên quản lý nên handledBy = null (mặc định)

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

        for (OrderRequest.CartItem item : request.getItems()) {
            DishVariant variant = dishVariantRepository.findById(item.getVariantId())
                    .orElseThrow(() -> new RuntimeException("Món/Size ID " + item.getVariantId() + " không tồn tại"));

            if (!variant.getDish().isAvailable()) {
                throw new RuntimeException("Món " + variant.getDish().getName() + " hiện đã ngưng phục vụ!");
            }

            boolean hasToppings = item.getToppingIds() != null && !item.getToppingIds().isEmpty();
            String categoryName = variant.getDish().getCategory().getName();

            if (hasToppings && !categoryName.equalsIgnoreCase("Pizza")) {
                throw new RuntimeException("Chỉ có thể thêm Topping cho món Pizza. Món '"
                        + variant.getDish().getName() + "' không hỗ trợ.");
            }

            OrderDetail detail = new OrderDetail();
            detail.setDishVariant(variant);
            detail.setQuantity(item.getQuantity());

            double toppingsPrice = 0;
            List<Topping> toppings = new ArrayList<>();

            if (item.getToppingIds() != null && !item.getToppingIds().isEmpty()) {
                toppings = toppingRepository.findAllById(item.getToppingIds());
                detail.setToppings(toppings);

                for (Topping t : toppings) {
                    toppingsPrice += t.getPrice();
                }
            }

            double finalUnitPrice = variant.getPrice() + toppingsPrice;
            detail.setUnitPrice(finalUnitPrice);
            double subTotal = finalUnitPrice * item.getQuantity();
            detail.setSubTotal(subTotal);

            totalAmount += subTotal;
            order.addDetail(detail);
        }

        order.setTotalPrice(totalAmount);

        double discountAmount = 0;

        if (request.getCouponCode() != null && !request.getCouponCode().trim().isEmpty()) {
            String code = request.getCouponCode().toUpperCase();

            Coupon coupon = couponRepository.findByCode(code)
                    .orElseThrow(() -> new RuntimeException("Mã giảm giá không tồn tại!"));

            if (coupon == null) throw new RuntimeException("Mã giảm giá không tồn tại!");

            LocalDate today = LocalDate.now();

            if (!coupon.isActive()) throw new RuntimeException("Mã giảm giá đang bị khóa!");

            if (coupon.getExpirationDate() != null && today.isAfter(coupon.getExpirationDate())) {
                throw new RuntimeException("Mã giảm giá đã hết hạn vào ngày " + coupon.getExpirationDate());
            }

            if (coupon.getUsageLimit() != null && coupon.getUsageCount() >= coupon.getUsageLimit()) {
                throw new RuntimeException("Mã giảm giá đã hết lượt sử dụng!");
            }

            if (coupon.getMinOrderAmount() != null && totalAmount < coupon.getMinOrderAmount()) {
                throw new RuntimeException("Đơn hàng chưa đủ giá trị tối thiểu ("
                        + String.format("%,.0f", coupon.getMinOrderAmount()) + " đ) để dùng mã này!");
            }

            long usedCount = orderRepository.countUsedByCustomer(customer.getId(), code);
            if (usedCount > 0) {
                throw new RuntimeException("Bạn đã sử dụng mã này rồi (Mỗi khách chỉ được dùng 1 lần)!");
            }

            if (coupon.getDiscountPercent() != null && coupon.getDiscountPercent() > 0) {
                discountAmount = totalAmount * (coupon.getDiscountPercent() / 100.0);
                if (coupon.getMaxDiscountAmount() != null && discountAmount > coupon.getMaxDiscountAmount()) {
                    discountAmount = coupon.getMaxDiscountAmount();
                }
            }
            else if (coupon.getDiscountAmount() != null && coupon.getDiscountAmount() > 0) {
                discountAmount = coupon.getDiscountAmount();
            }

            if (discountAmount > totalAmount) {
                discountAmount = totalAmount;
            }

            coupon.setUsageCount(coupon.getUsageCount() + 1);
            couponRepository.save(coupon);

            order.setCouponCode(code);
            order.setDiscountAmount(discountAmount);
        }

        double finalTotal = totalAmount - discountAmount;
        order.setFinalTotalPrice(finalTotal);

        return orderRepository.save(order);
    }

    // [2] SỬA LẠI HÀM NÀY: THÊM THAM SỐ staffUsername
    @Transactional
    public void approveOrder(Long orderId, String staffUsername) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PAID) {
            throw new RuntimeException("Đơn hàng này không thể duyệt (đang nấu hoặc đã hủy)!");
        }

        // --- [3] TÌM VÀ GÁN NHÂN VIÊN XỬ LÝ ---
        Employee staff = employeeRepository.findByAccount_Username(staffUsername)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin nhân viên: " + staffUsername));

        order.setHandledBy(staff); // Gán nhân viên vào đơn hàng
        // -------------------------------------

        // LOGIC TRỪ KHO (Giữ nguyên)
        for (OrderDetail detail : order.getOrderDetails()) {
            DishVariant variant = detail.getDishVariant();
            int quantityOrdered = detail.getQuantity();

            // A. TRỪ KHO THEO CÔNG THỨC MÓN ĂN
            for (Recipe recipe : variant.getRecipes()) {
                Product product = recipe.getProduct();
                double totalNeeded = recipe.getQuantityNeeded() * quantityOrdered;

                if (product.getStockQuantity() < totalNeeded) {
                    throw new RuntimeException("Không đủ nguyên liệu món chính: " + product.getName());
                }
                product.setStockQuantity(product.getStockQuantity() - totalNeeded);
                productRepository.save(product);
            }

            // B. TRỪ KHO THEO TOPPING
            if (detail.getToppings() != null) {
                for (Topping topping : detail.getToppings()) {
                    Product product = topping.getProduct();
                    double totalNeeded = topping.getQuantityNeeded() * quantityOrdered;

                    if (product.getStockQuantity() < totalNeeded) {
                        throw new RuntimeException("Thiếu nguyên liệu cho Topping: " + topping.getName());
                    }

                    product.setStockQuantity(product.getStockQuantity() - totalNeeded);
                    productRepository.save(product);
                }
            }

            dishService.refreshDishAvailability(variant.getDish());
        }

        order.setStatus(OrderStatus.COOKING);
        orderRepository.save(order);
    }
}