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

import java.util.List;

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

    // 1. TẠO ĐƠN HÀNG (KHÁCH HÀNG)
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> placeOrder(@RequestBody OrderRequest request) {
        try {
            String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();

            Order newOrder = orderService.createOrder(currentUsername, request);

            return ResponseEntity.ok(newOrder);

        } catch (RuntimeException e) {
            // Lỗi nghiệp vụ (Hết hàng, Coupon sai...)
            return ResponseEntity.badRequest().body("Không thể đặt hàng: " + e.getMessage());
        } catch (Exception e){
            return ResponseEntity.status(500).body("Lỗi hệ thống: " + e.getMessage());
        }
    }


    // 2. LẤY ĐƠN HÀNG CHO BẾP
    @GetMapping("/kitchen")
    @PreAuthorize("hasAnyRole('CHEF', 'MANAGER', 'STAFF')")
    public ResponseEntity<?> getKitchenOrders() {
        return ResponseEntity.ok(orderRepository.findAll());
    }
    
    // 3. DUYỆT ĐƠN (CONFIRM)
    // Logic: Gán nhân viên chịu trách nhiệm. Không trừ kho nữa (vì đã trừ lúc tạo).
    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('CHEF', 'STAFF', 'MANAGER')")
    public ResponseEntity<?> confirmOrder(@PathVariable Long id) {
        try {
            String currentStaffUsername = SecurityContextHolder.getContext().getAuthentication().getName();

            orderService.approveOrder(id, currentStaffUsername);

            return ResponseEntity.ok("Đã xác nhận đơn hàng (Người duyệt: " + currentStaffUsername + ")");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    // 4. CẬP NHẬT TIẾN ĐỘ (STATUS UPDATE)
    // chuyển tiếp (COOKING -> DELIVERING -> COMPLETED).
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('CHEF', 'STAFF')")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam OrderStatus status) {
        try {
            // CHẶN HỦY ĐƠN Ở ĐÂY
            if (status == OrderStatus.CANCELLED) {
                return ResponseEntity.badRequest().body("Lỗi: Để hủy đơn, vui lòng dùng API /api/orders/{id}/cancel để đảm bảo hoàn kho!");
            }

            Order order = orderRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng!"));

            order.setStatus(status);
            orderRepository.save(order);
            return ResponseEntity.ok("Đã cập nhật trạng thái thành: " + status);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    // 5. HỦY ĐƠN & HOÀN KHO (CANCEL)
    // Logic: Gọi Service để cộng lại nguyên liệu vào kho.
    @PutMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id, @RequestParam(required = false) String reason) {
        try {
            String cancelReason = (reason != null && !reason.isEmpty()) ? reason : "Không có lý do";

            // Gọi hàm hoàn kho bên Service
            orderService.cancelOrder(id, cancelReason);

            return ResponseEntity.ok("Đã hủy đơn hàng và hoàn lại nguyên liệu vào kho thành công!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Không thể hủy đơn: " + e.getMessage());
        }
    }


    // 6. LẤY LỊCH SỬ ĐƠN CỦA KHÁCH
    @GetMapping("/my-orders")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> getMyOrders() {
        try {
            String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
            List<Order> orders = orderRepository.findByCustomer_Account_UsernameOrderByOrderTimeDesc(currentUsername);

            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }
}