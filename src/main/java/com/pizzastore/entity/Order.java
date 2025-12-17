package com.pizzastore.entity;

import com.pizzastore.enums.OrderStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.pizzastore.enums.DeliveryMethod;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime orderTime;

    // Tổng tiền gốc (Chưa giảm giá)
    private Double totalPrice;

    // --- CÁC TRƯỜNG MỚI THÊM VÀO ---
    private String couponCode;       // Mã giảm giá đã dùng
    private Double discountAmount;   // Số tiền được giảm (VD: 50000.0)
    private Double finalTotalPrice;  // Tổng tiền phải trả sau khi trừ khuyến mãi
    // --------------------------------

    private String note; // Ghi chú của khách (ít cay, không hành...)
    private String deliveryAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_method")
    private DeliveryMethod deliveryMethod;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    // Ai đặt đơn này?
    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    // Chi tiết món ăn (Cascade ALL: Lưu Order tự lưu luôn Details)
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderDetail> orderDetails = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = true)
    private Employee handledBy;

    // --- Constructor ---
    public Order() {}

    // Helper method để thêm món vào list cho dễ
    public void addDetail(OrderDetail detail) {
        orderDetails.add(detail);
        detail.setOrder(this);
    }


    // --- Getter & Setter ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDateTime getOrderTime() { return orderTime; }
    public void setOrderTime(LocalDateTime orderTime) { this.orderTime = orderTime; }

    public Double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(Double totalPrice) { this.totalPrice = totalPrice; }

    // --- GETTER/SETTER CHO 3 TRƯỜNG MỚI ---
    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }

    public Double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(Double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public Double getFinalTotalPrice() {
        return finalTotalPrice;
    }

    public void setFinalTotalPrice(Double finalTotalPrice) {
        this.finalTotalPrice = finalTotalPrice;
    }
    // ---------------------------------------

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    public List<OrderDetail> getOrderDetails() { return orderDetails; }
    public void setOrderDetails(List<OrderDetail> orderDetails) { this.orderDetails = orderDetails; }

    public DeliveryMethod getDeliveryMethod() { return deliveryMethod; }
    public void setDeliveryMethod(DeliveryMethod deliveryMethod) { this.deliveryMethod = deliveryMethod; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String address) { this.deliveryAddress = address; }

    // Getter & Setter
    public Employee getHandledBy() { return handledBy; }
    public void setHandledBy(Employee handledBy) { this.handledBy = handledBy; }
}