package com.pizzastore.entity;

import com.pizzastore.enums.OrderStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime orderTime;
    private Double totalPrice;
    private String note; // Ghi chú của khách (ít cay, không hành...)

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    // Ai đặt đơn này?
    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    // Chi tiết món ăn (Cascade ALL: Lưu Order tự lưu luôn Details)
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderDetail> orderDetails = new ArrayList<>();

    // --- Constructor, Getter, Setter ---
    public Order() {}

    // Helper method để thêm món vào list cho dễ
    public void addDetail(OrderDetail detail) {
        orderDetails.add(detail);
        detail.setOrder(this);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getOrderTime() { return orderTime; }
    public void setOrderTime(LocalDateTime orderTime) { this.orderTime = orderTime; }
    public Double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(Double totalPrice) { this.totalPrice = totalPrice; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public List<OrderDetail> getOrderDetails() { return orderDetails; }
    public void setOrderDetails(List<OrderDetail> orderDetails) { this.orderDetails = orderDetails; }
}