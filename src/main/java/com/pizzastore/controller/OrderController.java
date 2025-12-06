package com.pizzastore.controller;

import com.pizzastore.dto.OrderRequest;
import com.pizzastore.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.pizzastore.entity.Order;
import com.pizzastore.enums.OrderStatus;
import com.pizzastore.repository.OrderRepository;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;

    @Autowired
    public OrderController(OrderService orderService, OrderRepository orderRepository) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
    }

    // API: Khách hàng đặt món
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')") // Chỉ khách hàng mới được đặt
    public ResponseEntity<?> placeOrder(@RequestBody OrderRequest request) {
        try {
            // Lấy username người đang đăng nhập
            String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();

            String result = orderService.createOrder(currentUsername, request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi đặt hàng: " + e.getMessage());
        }
    }

    // --- 1. LẤY ĐƠN HÀNG CẦN NẤU (Cho Bếp) ---
    @GetMapping("/kitchen")
    @PreAuthorize("hasAnyRole('CHEF', 'MANAGER', 'STAFF')")
    public ResponseEntity<?> getKitchenOrders() {
        // Lấy tất cả đơn có trạng thái PENDING hoặc CONFIRMED
        // (Bạn cần viết thêm hàm findByStatusIn trong Repository)
        return ResponseEntity.ok(orderRepository.findAll());
    }

    // --- 2. DUYỆT ĐƠN & NẤU (Kích hoạt trừ kho) ---
    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('CHEF', 'STAFF')")
    public ResponseEntity<?> confirmOrder(@PathVariable Long id) {
        try {
            orderService.approveOrder(id);
            return ResponseEntity.ok("Đã xác nhận đơn hàng và trừ kho thành công!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi kho: " + e.getMessage());
        }
    }

    // --- 3. CẬP NHẬT TRẠNG THÁI (Nấu xong -> Giao) ---
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('CHEF', 'STAFF')")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam OrderStatus status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng!"));

        order.setStatus(status);
        orderRepository.save(order);
        return ResponseEntity.ok("Cập nhật trạng thái thành: " + status);
    }
}