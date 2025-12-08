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
        int paymentStatus = paymentService.orderReturn(request);

        String redirectUrl = "";
        if (paymentStatus == 1) {
            // Thanh toán thành công -> Chuyển hướng về trang Frontend "Cảm ơn"
            // Thay đổi link này thành link Frontend của bạn (VD: localhost:3000/success)
            redirectUrl = "http://localhost:3000/payment-success";
        } else {
            // Thất bại
            redirectUrl = "http://localhost:3000/payment-failed";
        }

        response.sendRedirect(redirectUrl);
    }
}