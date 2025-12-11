package com.pizzastore.controller;

import com.pizzastore.dto.PaymentResponse; // <--- Import file vừa tạo
import com.pizzastore.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletResponse; // <--- Để dùng response.sendRedirect
import java.io.IOException;                      // <--- Để xử lý lỗi IOException

@RestController
@RequestMapping("/api/payment")
public class PaymentController {
    @Autowired
    private PaymentService paymentService;

    @GetMapping("/create_payment")
    public ResponseEntity<?> createPayment(HttpServletRequest request,
                                           @RequestParam Long orderId) { // Bỏ @RequestParam amount

        // Gọi service (không truyền amount nữa)
        String paymentUrl = paymentService.createVnPayPayment(request, orderId);

        return ResponseEntity.ok(new PaymentResponse("OK", "URL thanh toán", paymentUrl));
    }
    // API CALLBACK (VNPAY gọi về đây)
    @GetMapping("/vnpay-callback")
    public void paymentCallback(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 1. Xử lý cập nhật database
        int paymentStatus = paymentService.orderReturn(request);

        // 2. Cấu hình URL Frontend (Sửa port thành 5173)
        String frontendUrl = "http://localhost:5173";

        if (paymentStatus == 1) {
            // --- THANH TOÁN THÀNH CÔNG ---

            // Lấy Order ID từ dữ liệu VNPAY gửi về (để FE biết đơn nào)
            String orderId = request.getParameter("vnp_OrderInfo");

            // Redirect về trang Success kèm Order ID
            // URL sẽ là: http://localhost:5173/payment-success?orderId=5
            response.sendRedirect(frontendUrl + "/payment-success?orderId=" + orderId);

        } else {
            // --- THẤT BẠI ---
            response.sendRedirect(frontendUrl + "/payment-failed");
        }
    }
}