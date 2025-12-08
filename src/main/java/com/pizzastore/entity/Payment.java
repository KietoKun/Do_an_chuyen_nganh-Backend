package com.pizzastore.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pizzastore.enums.PaymentMethod;
import com.pizzastore.enums.PaymentStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Một đơn hàng chỉ có 1 thanh toán (trong mô hình đơn giản)
    @OneToOne
    @JoinColumn(name = "order_id")
    @JsonIgnore
    private Order order;

    private LocalDateTime paymentTime;

    private Double amount; // Số tiền thanh toán

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private String transactionCode; // Mã giao dịch của VNPAY trả về (để đối soát)

    // --- Constructor, Getter, Setter ---
    public Payment() {}

    // (Bạn tự generate Getter/Setter nhé)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
    public LocalDateTime getPaymentTime() { return paymentTime; }
    public void setPaymentTime(LocalDateTime paymentTime) { this.paymentTime = paymentTime; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public String getTransactionCode() { return transactionCode; }
    public void setTransactionCode(String transactionCode) { this.transactionCode = transactionCode; }
}