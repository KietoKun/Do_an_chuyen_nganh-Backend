package com.pizzastore.service.unit;

import com.pizzastore.config.VnPayConfig;
import com.pizzastore.entity.Order;
import com.pizzastore.entity.Payment;
import com.pizzastore.enums.OrderStatus;
import com.pizzastore.enums.PaymentStatus;
import com.pizzastore.repository.OrderRepository;
import com.pizzastore.repository.PaymentRepository;
import com.pizzastore.service.OrderRealtimeService;
import com.pizzastore.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private VnPayConfig vnPayConfig;
    @Mock private OrderRepository orderRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private OrderRealtimeService orderRealtimeService;

    // Giả mạo (Mock) HTTP Request của máy chủ
    @Mock private HttpServletRequest request;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void createVnPayPayment_ShouldThrowException_WhenOrderNotFound() {
        // ARRANGE: Cố tình truyền vào ID không tồn tại
        Long wrongOrderId = 99L;
        when(orderRepository.findById(wrongOrderId)).thenReturn(Optional.empty());

        // ACT & ASSERT: Kỳ vọng ném ra lỗi RuntimeException
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            paymentService.createVnPayPayment(request, wrongOrderId);
        });

        assertEquals("Đơn hàng không tồn tại", exception.getMessage());
    }

    @Test
    void orderReturn_ShouldReturn1AndSavePayment_WhenPaymentIsSuccessful() {
        // 1. ARRANGE: Giả lập VNPay gửi request trả về với kết quả thành công
        when(request.getParameterNames()).thenReturn(Collections.enumeration(Arrays.asList(
                "vnp_Amount", "vnp_ResponseCode", "vnp_SecureHash", "vnp_OrderInfo", "vnp_TransactionNo"
        )));
        when(request.getParameter("vnp_Amount")).thenReturn("15000000"); // 150k
        when(request.getParameter("vnp_ResponseCode")).thenReturn("00"); // 00 là mã thành công của VNPay
        when(request.getParameter("vnp_SecureHash")).thenReturn("correct_hash");
        when(request.getParameter("vnp_OrderInfo")).thenReturn("1");
        when(request.getParameter("vnp_TransactionNo")).thenReturn("123456789");

        // Giả lập hàm băm (hash) của hệ thống khớp với hash do VNPay gửi tới
        when(vnPayConfig.hashAllFields(anyMap())).thenReturn("correct_hash");

        Order mockOrder = new Order();
        mockOrder.setId(1L);
        mockOrder.setStatus(OrderStatus.PENDING);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(mockOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(mockOrder);

        // 2. ACT
        int result = paymentService.orderReturn(request);

        // 3. ASSERT
        assertEquals(1, result); // 1 = Thành công
        assertEquals(OrderStatus.PAID, mockOrder.getStatus()); // Đơn hàng phải được cập nhật thành PAID

        // Kiểm tra xem lịch sử Payment đã được lưu xuống DB chưa
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, times(1)).save(paymentCaptor.capture());
        Payment savedPayment = paymentCaptor.getValue();

        assertEquals(PaymentStatus.SUCCESS, savedPayment.getStatus());
        assertEquals(150000.0, savedPayment.getAmount()); // 15000000 / 100

        // Đảm bảo thông báo realtime đã được bắn đi
        verify(orderRealtimeService, times(1)).publishOrderPaymentUpdated(eq(mockOrder), eq(OrderStatus.PENDING), anyString());
    }

    @Test
    void orderReturn_ShouldReturnMinus1_WhenSignatureIsInvalid() {
        // ARRANGE: Kẻ gian cố tình sửa đổi URL, làm sai lệch mã băm (SecureHash)
        when(request.getParameterNames()).thenReturn(Collections.enumeration(Arrays.asList("vnp_SecureHash")));
        when(request.getParameter("vnp_SecureHash")).thenReturn("fake_hash_from_hacker");

        // Hash thật do server tính ra khác với hash giả
        when(vnPayConfig.hashAllFields(anyMap())).thenReturn("real_system_hash");

        // ACT
        int result = paymentService.orderReturn(request);

        // ASSERT
        assertEquals(-1, result); // -1 = Chữ ký không hợp lệ
        // Không lưu bất cứ thứ gì xuống DB
        verify(paymentRepository, never()).save(any());
    }
}
