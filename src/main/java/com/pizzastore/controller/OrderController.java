package com.pizzastore.controller;

import com.pizzastore.dto.OrderHistoryResponse;
import com.pizzastore.dto.OrderRequest;
import com.pizzastore.entity.Order;
import com.pizzastore.enums.OrderStatus;
import com.pizzastore.repository.OrderRepository;
import com.pizzastore.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "7. Quan ly Don hang (Order)", description = "API dat hang, xu ly don, cap nhat trang thai va huy don")
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;

    @Autowired
    public OrderController(OrderService orderService, OrderRepository orderRepository) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Tao don hang moi (Khach hang)", description = "Khach hang gui gio hang va thong tin giao nhan de tao don moi.")
    public ResponseEntity<?> placeOrder(@RequestBody OrderRequest request) {
        try {
            String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
            Order newOrder = orderService.createOrder(currentUsername, request);
            return ResponseEntity.ok(newOrder);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Không thể đặt hàng: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @GetMapping("/kitchen")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CHEF', 'MANAGER', 'STAFF')")
    @Operation(summary = "Lay danh sach don hang (Noi bo)", description = "Danh cho bep hoac nhan vien xem danh sach don hang de xu ly.")
    public ResponseEntity<?> getKitchenOrders(@RequestParam(required = false) Long branchId) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(orderService.getInternalOrders(currentUsername, branchId));
    }

    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'STAFF', 'MANAGER')")
    @Operation(summary = "Duyet / tiep nhan don hang", description = "Nhan vien xac nhan don va chuyen trang thai sang CONFIRMED.")
    public ResponseEntity<?> confirmOrder(@PathVariable Long id) {
        try {
            String currentStaffUsername = SecurityContextHolder.getContext().getAuthentication().getName();
            orderService.approveOrder(id, currentStaffUsername);
            return ResponseEntity.ok("Da xac nhan don hang (Nguoi duyet: " + currentStaffUsername + ")");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/start-cooking")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CHEF')")
    @Operation(summary = "Bep bat dau nau don", description = "Dau bep nhan don da duoc staff xac nhan va chuyen trang thai sang COOKING.")
    public ResponseEntity<?> startCooking(@PathVariable Long id) {
        try {
            String currentChefUsername = SecurityContextHolder.getContext().getAuthentication().getName();
            orderService.startCooking(id, currentChefUsername);
            return ResponseEntity.ok("Da bat dau nau don hang (Dau bep: " + currentChefUsername + ")");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lá»—i: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CHEF', 'STAFF')")
    @Operation(summary = "Cap nhat tien do don hang", description = "Chuyen trang thai don hang ma khong dung cho huy don.")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam OrderStatus status) {
        try {
            if (status == OrderStatus.CANCELLED) {
                return ResponseEntity.badRequest().body("Lỗi: Để hủy đơn, vui lòng dùng API /api/orders/{id}/cancel");
            }

            String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
            orderService.updateOrderStatus(id, status, currentUsername);
            return ResponseEntity.ok("Da cap nhat trang thai thanh: " + status);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Huy don va hoan kho", description = "Huy don hang va hoan lai nguyen lieu vao kho.")
    public ResponseEntity<?> cancelOrder(@PathVariable Long id, @RequestParam(required = false) String reason) {
        try {
            String cancelReason = (reason != null && !reason.isEmpty()) ? reason : "Không có lý do";
            String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
            orderService.cancelOrder(id, cancelReason, currentUsername);
            return ResponseEntity.ok("Đã hủy đơn hàng và hoàn lại nguyên liệu vào kho thành công");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Không thể hủy đơn: " + e.getMessage());
        }
    }

    @GetMapping("/my-orders")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Xem lich su don hang ca nhan", description = "Khach hang xem cac don hang da dat.")
    public ResponseEntity<?> getMyOrders() {
        try {
            String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
            List<OrderHistoryResponse> orders = orderRepository.findByCustomer_Account_UsernameOrderByOrderTimeDesc(currentUsername)
                    .stream()
                    .map(OrderHistoryResponse::from)
                    .toList();
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }
}
