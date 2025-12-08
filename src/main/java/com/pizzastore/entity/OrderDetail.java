package com.pizzastore.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "order_details")
public class OrderDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- THAY ĐỔI QUAN TRỌNG: Dùng DishVariant thay vì Dish ---
    @ManyToOne
    @JoinColumn(name = "variant_id") // Đổi tên cột trong DB thành variant_id
    private DishVariant dishVariant;

    private Double unitPrice;
    private Integer quantity;
    private Double subTotal;

    @ManyToOne
    @JoinColumn(name = "order_id")
    @JsonIgnore
    private Order order;

    @ManyToMany
    @JoinTable(
            name = "order_detail_toppings",
            joinColumns = @JoinColumn(name = "order_detail_id"),
            inverseJoinColumns = @JoinColumn(name = "topping_id")
    )
    private java.util.List<Topping> toppings = new java.util.ArrayList<>();

    // --- CONSTRUCTOR ---
    public OrderDetail() {}

    // --- GETTERS & SETTERS (Cần cập nhật phần này) ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    // Getter/Setter cho DishVariant (Service đang báo lỗi vì thiếu cái này)
    public DishVariant getDishVariant() {
        return dishVariant;
    }

    public void setDishVariant(DishVariant dishVariant) {
        this.dishVariant = dishVariant;
    }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Double getSubTotal() { return subTotal; }
    public void setSubTotal(Double subTotal) { this.subTotal = subTotal; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public java.util.List<Topping> getToppings() { return toppings; }
    public void setToppings(java.util.List<Topping> toppings) { this.toppings = toppings; }

    public void setUnitPrice(Double unitPrice) {
        this.unitPrice = unitPrice;
    }
    public Double getUnitPrice() {
        return unitPrice;
    }
}