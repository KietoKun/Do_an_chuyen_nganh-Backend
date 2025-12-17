package com.pizzastore.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "coupons")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 20)
    private String code;            // Mã giảm giá (VD: WELCOME)

    @Column(name = "discount_percent")
    private Double discountPercent; // Giảm theo % (VD: 10.0)

    @Column(name = "discount_amount")
    private Double discountAmount;  // Giảm theo tiền mặt (VD: 50000.0)

    // [MỚI] Quan trọng để tránh lỗ vốn khi giảm %
    @Column(name = "max_discount_amount")
    private Double maxDiscountAmount;

    @Column(name = "expiration_date")
    private LocalDate expirationDate; // Ngày hết hạn

    @Column(name = "min_order_amount")
    private Double minOrderAmount;    // Giá trị đơn tối thiểu để dùng mã

    @Column(name = "usage_limit")
    private Integer usageLimit;       // Giới hạn số lần dùng toàn server

    @Column(name = "usage_count")
    private Integer usageCount = 0;   // Số lần đã dùng

    @Column(name = "active")
    private boolean active = true;    // Trạng thái kích hoạt

    // ==========================================
    // 1. CONSTRUCTORS
    // ==========================================

    public Coupon() {
    }

    // Constructor đầy đủ (bao gồm cả maxDiscountAmount)
    public Coupon(Long id, String code, Double discountPercent, Double discountAmount, Double maxDiscountAmount,
                  LocalDate expirationDate, Double minOrderAmount,
                  Integer usageLimit, Integer usageCount, boolean active) {
        this.id = id;
        this.code = code;
        this.discountPercent = discountPercent;
        this.discountAmount = discountAmount;
        this.maxDiscountAmount = maxDiscountAmount;
        this.expirationDate = expirationDate;
        this.minOrderAmount = minOrderAmount;
        this.usageLimit = usageLimit;
        this.usageCount = usageCount;
        this.active = active;
    }

    // ==========================================
    // 2. GETTER & SETTER (Thủ công)
    // ==========================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Double getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(Double discountPercent) { this.discountPercent = discountPercent; }

    public Double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(Double discountAmount) { this.discountAmount = discountAmount; }

    public Double getMaxDiscountAmount() { return maxDiscountAmount; }
    public void setMaxDiscountAmount(Double maxDiscountAmount) { this.maxDiscountAmount = maxDiscountAmount; }

    public LocalDate getExpirationDate() { return expirationDate; }
    public void setExpirationDate(LocalDate expirationDate) { this.expirationDate = expirationDate; }

    public Double getMinOrderAmount() { return minOrderAmount; }
    public void setMinOrderAmount(Double minOrderAmount) { this.minOrderAmount = minOrderAmount; }

    public Integer getUsageLimit() { return usageLimit; }
    public void setUsageLimit(Integer usageLimit) { this.usageLimit = usageLimit; }

    public Integer getUsageCount() { return usageCount; }
    public void setUsageCount(Integer usageCount) { this.usageCount = usageCount; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}