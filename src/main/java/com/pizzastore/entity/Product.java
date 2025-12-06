package com.pizzastore.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Double stockQuantity;
    private String unit;

    // --- 1. CONSTRUCTOR RỖNG (Bắt buộc) ---
    public Product() {}

    // --- 2. CONSTRUCTOR CÓ THAM SỐ (Cái bạn đang thiếu) ---
    public Product(String name, Double stockQuantity, String unit) {
        this.name = name;
        this.stockQuantity = stockQuantity;
        this.unit = unit;
    }

    // --- GETTER & SETTER ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Double getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Double stockQuantity) { this.stockQuantity = stockQuantity; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
}