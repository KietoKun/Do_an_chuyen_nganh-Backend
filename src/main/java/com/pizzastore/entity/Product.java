package com.pizzastore.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    // Đã loại bỏ stockQuantity vì số lượng giờ nằm ở bảng Inventory

    private String unit;

    public Product() {}

    // Constructor cập nhật: Chỉ nhận tên và đơn vị
    public Product(String name, String unit) {
        this.name = name;
        this.unit = unit;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
}