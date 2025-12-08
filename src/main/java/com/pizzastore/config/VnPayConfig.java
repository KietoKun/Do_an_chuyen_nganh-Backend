package com.pizzastore.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Random;

@Configuration
public class VnPayConfig {

    // Các biến cấu hình (Instance variables)
    private final String vnp_PayUrl;
    private final String vnp_ReturnUrl;
    private final String vnp_TmnCode;
    private final String vnp_HashSecret;
    private final String vnp_ApiUrl;

    // --- 1. CONSTRUCTOR (Inject giá trị từ application.yml) ---
    public VnPayConfig(
            @Value("${payment.vnpay.url}") String vnp_PayUrl,
            @Value("${payment.vnpay.return-url}") String vnp_ReturnUrl,
            @Value("${payment.vnpay.tmn-code}") String vnp_TmnCode,
            @Value("${payment.vnpay.hash-secret}") String vnp_HashSecret,
            @Value("${payment.vnpay.api-url}") String vnp_ApiUrl) {
        this.vnp_PayUrl = vnp_PayUrl;
        this.vnp_ReturnUrl = vnp_ReturnUrl;
        this.vnp_TmnCode = vnp_TmnCode;
        this.vnp_HashSecret = vnp_HashSecret;
        this.vnp_ApiUrl = vnp_ApiUrl;
    }

    // --- 2. GETTERS THỦ CÔNG (Thay thế cho @Getter của Lombok) ---
    public String getVnp_PayUrl() {
        return vnp_PayUrl;
    }

    public String getVnp_ReturnUrl() {
        return vnp_ReturnUrl;
    }

    public String getVnp_TmnCode() {
        return vnp_TmnCode;
    }

    public String getVnp_HashSecret() {
        return vnp_HashSecret;
    }

    public String getVnp_ApiUrl() {
        return vnp_ApiUrl;
    }

    // --- 3. CÁC HÀM TIỆN ÍCH (STATIC UTILS) ---

    public static String hmacSHA512(final String key, final String data) {
        try {
            if (key == null || data == null) {
                return null;
            }
            final Mac hmac512 = Mac.getInstance("HmacSHA512");
            byte[] hmacKeyBytes = key.getBytes();
            final SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
            hmac512.init(secretKey);
            byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
            byte[] result = hmac512.doFinal(dataBytes);
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            return "";
        }
    }

    public static String getIpAddress(HttpServletRequest request) {
        String ipAdress;
        try {
            ipAdress = request.getHeader("X-FORWARDED-FOR");
            if (ipAdress == null) {
                ipAdress = request.getRemoteAddr();
            }
        } catch (Exception e) {
            ipAdress = "Invalid IP:" + e.getMessage();
        }
        return ipAdress;
    }

    public static String getRandomNumber(int len) {
        Random rnd = new Random();
        String chars = "0123456789";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }
    public String hashAllFields(java.util.Map<String, String> fields) {
        // 1. Sắp xếp tham số theo A-Z
        java.util.List<String> fieldNames = new java.util.ArrayList<>(fields.keySet());
        java.util.Collections.sort(fieldNames);

        // 2. Tạo chuỗi dữ liệu
        StringBuilder sb = new StringBuilder();
        java.util.Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = fields.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                sb.append(fieldName);
                sb.append("=");
                sb.append(fieldValue);
            }
            if (itr.hasNext()) {
                sb.append("&");
            }
        }

        // 3. Mã hóa với Secret Key
        return hmacSHA512(this.vnp_HashSecret, sb.toString());
    }
}