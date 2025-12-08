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
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PaymentService {

    private final VnPayConfig vnPayConfig;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    @Autowired
    public PaymentService(VnPayConfig vnPayConfig, OrderRepository orderRepository, PaymentRepository paymentRepository) {
        this.vnPayConfig = vnPayConfig;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
    }

    // --- 1. TẠO URL THANH TOÁN (CẬP NHẬT: Lấy tiền từ DB) ---
    // Bỏ tham số 'amount' ở đầu vào, chỉ cần orderId
    public String createVnPayPayment(HttpServletRequest request, Long orderId) {

        // Bước 1: Tìm đơn hàng trong Database
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại!"));

        // Bước 2: Lấy tổng tiền từ đơn hàng (Nhân 100 theo yêu cầu của VNPAY)
        // Ví dụ: 100,000 VNĐ -> 10,000,000
        long amount = (long) (order.getTotalPrice() * 100);

        // Bước 3: Tạo tham số gửi sang VNPAY
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", vnPayConfig.getVnp_TmnCode());
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");

        // Mã giao dịch: OrderId + Random (để tránh trùng lặp nếu thanh toán lại)
        String vnp_TxnRef = orderId + "_" + VnPayConfig.getRandomNumber(4);
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);

        // Thông tin đơn hàng: Lưu OrderId để lúc Callback biết đơn nào
        vnp_Params.put("vnp_OrderInfo", String.valueOf(orderId));

        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnPayConfig.getVnp_ReturnUrl());
        vnp_Params.put("vnp_IpAddr", VnPayConfig.getIpAddress(request));

        // Thời gian tạo
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        // Thời gian hết hạn (15 phút)
        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        // --- Xây dựng URL & Checksum ---
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                // Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                try {
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    // Build query
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        String queryUrl = query.toString();
        String vnp_SecureHash = VnPayConfig.hmacSHA512(vnPayConfig.getVnp_HashSecret(), hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;

        return vnPayConfig.getVnp_PayUrl() + "?" + queryUrl;
    }

    // --- 2. XỬ LÝ CALLBACK (Kết quả trả về) ---
    public int orderReturn(HttpServletRequest request) {
        Map fields = new HashMap();
        for (Enumeration params = request.getParameterNames(); params.hasMoreElements();) {
            String fieldName = (String) params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                fields.put(fieldName, fieldValue);
            }
        }

        String vnp_SecureHash = request.getParameter("vnp_SecureHash");
        if (fields.containsKey("vnp_SecureHashType")) {
            fields.remove("vnp_SecureHashType");
        }
        if (fields.containsKey("vnp_SecureHash")) {
            fields.remove("vnp_SecureHash");
        }

        // Kiểm tra Chữ ký bảo mật
        String signValue = vnPayConfig.hashAllFields(fields);

        if (signValue.equals(vnp_SecureHash)) {
            if ("00".equals(request.getParameter("vnp_ResponseCode"))) {
                // GIAO DỊCH THÀNH CÔNG -> CẬP NHẬT DATABASE

                // Lấy Order ID từ vnp_OrderInfo
                String orderIdStr = request.getParameter("vnp_OrderInfo");
                Long orderId = Long.parseLong(orderIdStr);

                Order order = orderRepository.findById(orderId).orElse(null);
                if (order != null) {
                    // Cập nhật trạng thái đơn hàng
                    order.setStatus(OrderStatus.PAID);
                    orderRepository.save(order);

                    // Lưu lịch sử thanh toán
                    Payment payment = new Payment();
                    payment.setOrder(order);
                    payment.setPaymentTime(LocalDateTime.now());
                    payment.setPaymentMethod(PaymentMethod.VNPAY);
                    payment.setStatus(PaymentStatus.SUCCESS);
                    payment.setAmount(Double.parseDouble(request.getParameter("vnp_Amount")) / 100);
                    payment.setTransactionCode(request.getParameter("vnp_TransactionNo"));
                    paymentRepository.save(payment);
                }
                return 1; // Success
            } else {
                return 0; // Thất bại
            }
        } else {
            return -1; // Sai chữ ký
        }
    }
}