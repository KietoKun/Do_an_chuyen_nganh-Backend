package com.pizzastore.enums;

public enum OrderStatus {
    PENDING,    // Chờ xác nhận (COD)
    PAID,       // Đã thanh toán Online (VNPAY) <--- THÊM CÁI NÀY
    CONFIRMED,  // Đã xác nhận (Không dùng nhiều nữa nếu chuyển thẳng sang Cooking)
    COOKING,    // Đang nấu (Đã trừ kho)
    DELIVERING,
    COMPLETED,
    CANCELLED
}