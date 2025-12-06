package com.pizzastore.entity;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;

@Entity
@Table(name = "toppings")
public class Topping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;   // Tên: Thêm Phô Mai
    private Double price;  // Giá bán: 15.000

    // Topping này trừ vào nguyên liệu nào trong kho?
    @ManyToOne
    @JoinColumn(name = "product_id")
    @JsonIgnore // <--- 2. CHẶN KHÔNG CHO HIỆN KHO
    private Product product;

    @JsonIgnore // <--- 3. CHẶN KHÔNG CHO HIỆN CÔNG THỨC
    private Double quantityNeeded; // Cần bao nhiêu nguyên liệu

    // --- CONSTRUCTOR ---
    public Topping() {}

    // --- GETTERS & SETTERS ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Double getQuantityNeeded() { return quantityNeeded; }
    public void setQuantityNeeded(Double quantityNeeded) { this.quantityNeeded = quantityNeeded; }
}