package com.pizzastore.service;

import com.pizzastore.dto.OrderRequest;
import com.pizzastore.entity.*;
import com.pizzastore.enums.OrderStatus;
import com.pizzastore.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Autowired
    public OrderService(OrderRepository orderRepository,
                        DishVariantRepository dishVariantRepository,
                        CustomerRepository customerRepository,
                        AccountRepository accountRepository,
                        ProductRepository productRepository,
                        DishService dishService,
                        ToppingRepository toppingRepository) {
        this.orderRepository = orderRepository;
        this.dishVariantRepository = dishVariantRepository;
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.productRepository = productRepository;
        this.dishService = dishService;
        this.toppingRepository = toppingRepository;
    }

    @Transactional
    public String createOrder(String username, OrderRequest request) {
        // 1. Tìm Customer
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));

        Customer customer = customerRepository.findAll().stream()
                .filter(c -> c.getAccount().getId().equals(account.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin khách hàng"));

        // 2. Tạo Order
        Order order = new Order();
        order.setCustomer(customer);
        order.setOrderTime(LocalDateTime.now());
        order.setStatus(OrderStatus.PENDING);
        order.setNote(request.getNote());

        double totalAmount = 0;

        // 3. Duyệt từng món (Variant)
        for (OrderRequest.CartItem item : request.getItems()) {
            DishVariant variant = dishVariantRepository.findById(item.getVariantId())
                    .orElseThrow(() -> new RuntimeException("Món/Size ID " + item.getVariantId() + " không tồn tại"));

            if (!variant.getDish().isAvailable()) {
                throw new RuntimeException("Món " + variant.getDish().getName() + " hiện đã ngưng phục vụ!");
            }

            OrderDetail detail = new OrderDetail();
            detail.setDishVariant(variant);
            detail.setQuantity(item.getQuantity());

            // --- XỬ LÝ TOPPING ---
            double toppingsPrice = 0;
            List<Topping> toppings = new ArrayList<>();

            if (item.getToppingIds() != null && !item.getToppingIds().isEmpty()) {
                toppings = toppingRepository.findAllById(item.getToppingIds());
                detail.setToppings(toppings);

                for (Topping t : toppings) {
                    toppingsPrice += t.getPrice();
                }
            }

            // --- TÍNH TOÁN GIÁ SNAPSHOT (QUAN TRỌNG) ---

            // 1. Lấy giá gốc tại thời điểm hiện tại
            double currentVariantPrice = variant.getPrice();

            // 2. Tính Đơn giá tổng hợp (Giá món + Giá Topping)
            // Đây là giá của 1 cái bánh kèm topping lúc khách mua
            double finalUnitPrice = currentVariantPrice + toppingsPrice;

            // 3. Lưu giá này vào DB (Để sau này đối chiếu nếu giá menu thay đổi)
            detail.setUnitPrice(finalUnitPrice);

            // 4. Tính thành tiền (Số lượng * Đơn giá tổng hợp)
            double subTotal = finalUnitPrice * item.getQuantity();
            detail.setSubTotal(subTotal);

            totalAmount += subTotal;
            order.addDetail(detail);
        }

        order.setTotalPrice(totalAmount);
        orderRepository.save(order);

        return "Đặt hàng thành công! Mã đơn: " + order.getId() + ". Tổng tiền: " + totalAmount;
    }

    @Transactional
    public void approveOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Chỉ đơn hàng đang chờ mới được duyệt!");
        }

        // --- LOGIC TRỪ KHO ---
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