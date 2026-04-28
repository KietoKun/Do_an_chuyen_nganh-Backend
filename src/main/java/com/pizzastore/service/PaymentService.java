package com.pizzastore.service;

import com.pizzastore.config.VnPayConfig;
import com.pizzastore.entity.Order;
import com.pizzastore.entity.Payment;
import com.pizzastore.enums.OrderStatus;
import com.pizzastore.enums.PaymentMethod;
import com.pizzastore.enums.PaymentStatus;
import com.pizzastore.repository.OrderRepository;
import com.pizzastore.repository.PaymentRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

@Service
public class PaymentService {

    private final VnPayConfig vnPayConfig;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRealtimeService orderRealtimeService;

    @Autowired
    public PaymentService(VnPayConfig vnPayConfig,
                          OrderRepository orderRepository,
                          PaymentRepository paymentRepository,
                          OrderRealtimeService orderRealtimeService) {
        this.vnPayConfig = vnPayConfig;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.orderRealtimeService = orderRealtimeService;
    }

    public String createVnPayPayment(HttpServletRequest request, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));

        long amount = (long) (order.getTotalPrice() * 100);

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", vnPayConfig.getVnp_TmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(amount));
        vnpParams.put("vnp_CurrCode", "VND");

        String vnpTxnRef = orderId + "_" + VnPayConfig.getRandomNumber(4);
        vnpParams.put("vnp_TxnRef", vnpTxnRef);
        vnpParams.put("vnp_OrderInfo", String.valueOf(orderId));
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnPayConfig.getVnp_ReturnUrl());
        vnpParams.put("vnp_IpAddr", VnPayConfig.getIpAddress(request));

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        vnpParams.put("vnp_CreateDate", formatter.format(calendar.getTime()));

        calendar.add(Calendar.MINUTE, 15);
        vnpParams.put("vnp_ExpireDate", formatter.format(calendar.getTime()));

        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> iterator = fieldNames.iterator();
        while (iterator.hasNext()) {
            String fieldName = iterator.next();
            String fieldValue = vnpParams.get(fieldName);
            if (fieldValue == null || fieldValue.isEmpty()) {
                continue;
            }

            try {
                hashData.append(fieldName)
                        .append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));

                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()))
                        .append('=')
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
            } catch (Exception e) {
                throw new RuntimeException("Không thể tạo URL thanh toán", e);
            }

            if (iterator.hasNext()) {
                hashData.append('&');
                query.append('&');
            }
        }

        String vnpSecureHash = VnPayConfig.hmacSHA512(vnPayConfig.getVnp_HashSecret(), hashData.toString());
        return vnPayConfig.getVnp_PayUrl() + "?" + query + "&vnp_SecureHash=" + vnpSecureHash;
    }

    public int orderReturn(HttpServletRequest request) {
        Map<String, String> fields = new HashMap<>();
        for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements(); ) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                fields.put(fieldName, fieldValue);
            }
        }

        String vnpSecureHash = request.getParameter("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");
        fields.remove("vnp_SecureHash");

        String signValue = vnPayConfig.hashAllFields(fields);
        if (!signValue.equals(vnpSecureHash)) {
            return -1;
        }

        if (!"00".equals(request.getParameter("vnp_ResponseCode"))) {
            return 0;
        }

        Long orderId = Long.parseLong(request.getParameter("vnp_OrderInfo"));
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return 0;
        }

        OrderStatus previousStatus = order.getStatus();
        order.setStatus(OrderStatus.PAID);
        Order savedOrder = orderRepository.save(order);

        Payment payment = new Payment();
        payment.setOrder(savedOrder);
        payment.setPaymentTime(LocalDateTime.now());
        payment.setPaymentMethod(PaymentMethod.VNPAY);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setAmount(Double.parseDouble(request.getParameter("vnp_Amount")) / 100);
        payment.setTransactionCode(request.getParameter("vnp_TransactionNo"));
        paymentRepository.save(payment);

        orderRealtimeService.publishOrderPaymentUpdated(
                savedOrder,
                previousStatus,
                "Thanh toán VNPAY thành công"
        );

        return 1;
    }
}
