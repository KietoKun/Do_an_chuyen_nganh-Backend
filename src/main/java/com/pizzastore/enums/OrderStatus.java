package com.pizzastore.enums;

public enum OrderStatus {
    PENDING,    // Chờ xác nhận (Mới đặt)
    CONFIRMED,  // Đã xác nhận (Staff duyệt)
    COOKING,    // Đang nấu (Bếp đang làm)
    DELIVERING, // Đang giao/Đang phục vụ
    COMPLETED,  // Hoàn thành/Đã thanh toán
    CANCELLED   // Đã hủy
}