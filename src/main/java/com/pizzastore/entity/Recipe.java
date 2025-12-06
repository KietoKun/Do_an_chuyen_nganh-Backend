package com.pizzastore.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "recipes")
public class Recipe {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- 1. SỬA LIÊN KẾT: Trỏ tới DishVariant (Size) thay vì Dish chung ---
    @ManyToOne
    @JoinColumn(name = "variant_id") // Đặt tên cột khóa ngoại là variant_id cho rõ nghĩa
    @JsonIgnore
    private DishVariant dishVariant;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    private Double quantityNeeded;

    // ==========================================
    // CONSTRUCTORS
    // ==========================================

    public Recipe() {
    }

    // --- 2. SỬA CONSTRUCTOR: Nhận vào DishVariant ---
    public Recipe(DishVariant dishVariant, Product product, Double quantityNeeded) {
        this.dishVariant = dishVariant;
        this.product = product;
        this.quantityNeeded = quantityNeeded;
    }

    // ==========================================
    // GETTERS & SETTERS
    // ==========================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // --- 3. SỬA GETTER/SETTER: Đổi Dish thành DishVariant ---
    public DishVariant getDishVariant() {
        return dishVariant;
    }

    public void setDishVariant(DishVariant dishVariant) {
        this.dishVariant = dishVariant;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Double getQuantityNeeded() {
        return quantityNeeded;
    }

    public void setQuantityNeeded(Double quantityNeeded) {
        this.quantityNeeded = quantityNeeded;
    }
}