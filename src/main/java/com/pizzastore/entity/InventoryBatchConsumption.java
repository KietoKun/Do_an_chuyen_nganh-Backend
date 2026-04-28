package com.pizzastore.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_batch_consumptions",
        indexes = {
                @Index(name = "idx_inventory_batch_consumptions_order_detail", columnList = "order_detail_id"),
                @Index(name = "idx_inventory_batch_consumptions_batch", columnList = "batch_id")
        })
public class InventoryBatchConsumption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_detail_id", nullable = false)
    @JsonIgnore
    private OrderDetail orderDetail;

    @ManyToOne
    @JoinColumn(name = "batch_id", nullable = false)
    private InventoryBatch batch;

    @Column(nullable = false)
    private Double quantity;

    @Column(nullable = false)
    private LocalDateTime consumedAt;

    private boolean returned = false;

    private LocalDateTime returnedAt;

    public InventoryBatchConsumption() {}

    public InventoryBatchConsumption(OrderDetail orderDetail, InventoryBatch batch, Double quantity) {
        this.orderDetail = orderDetail;
        this.batch = batch;
        this.quantity = quantity;
        this.consumedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public OrderDetail getOrderDetail() { return orderDetail; }
    public void setOrderDetail(OrderDetail orderDetail) { this.orderDetail = orderDetail; }

    public InventoryBatch getBatch() { return batch; }
    public void setBatch(InventoryBatch batch) { this.batch = batch; }

    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }

    public LocalDateTime getConsumedAt() { return consumedAt; }
    public void setConsumedAt(LocalDateTime consumedAt) { this.consumedAt = consumedAt; }

    public boolean isReturned() { return returned; }
    public void setReturned(boolean returned) { this.returned = returned; }

    public LocalDateTime getReturnedAt() { return returnedAt; }
    public void setReturnedAt(LocalDateTime returnedAt) { this.returnedAt = returnedAt; }
}
