package com.pizzastore.controller;

import com.pizzastore.dto.PaymentResponse;
import com.pizzastore.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequestMapping("/api/payment")
@Tag(name = "8. Thanh toán trực tuyến (VNPAY)", description = "Tích hợp cổng thanh toán VNPAY cho hệ thống")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @GetMapping("/create_payment")
    @Operation(summary = "Tạo URL Thanh toán VNPAY", description = "Gọi API này truyền vào Order ID để hệ thống tạo đường link an toàn. Frontend sẽ dùng link này mở trang thanh toán của VNPAY cho khách.")
    public ResponseEntity<?> createPayment(HttpServletRequest request,
                                           @RequestParam Long orderId) {

        String paymentUrl = paymentService.createVnPayPayment(request, orderId);

        return ResponseEntity.ok(new PaymentResponse("OK", "URL thanh toán", paymentUrl));
    }

    @GetMapping({"/vnpay-callback", "/vnpay_return"})
    @Operation(summary = "Webhook VNPAY Callback (Hệ thống tự gọi)", description = "API này dành riêng cho server VNPAY gọi về sau khi khách thanh toán xong để đối soát và cập nhật trạng thái đơn hàng.")
    public void paymentCallback(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 1. Xử lý cập nhật database
        int paymentStatus = paymentService.orderReturn(request);

        // 2. Cấu hình URL Frontend (Sửa port thành 5173)
        String frontendUrl = "http://localhost:5173";

        if (paymentStatus == 1) {
            // --- THANH TOÁN THÀNH CÔNG ---
            String orderId = request.getParameter("vnp_OrderInfo");
            response.sendRedirect(frontendUrl + "/payment-success?orderId=" + orderId);

        } else {
            // --- THẤT BẠI ---
            response.sendRedirect(frontendUrl + "/payment-failed");
        }
    }
}
