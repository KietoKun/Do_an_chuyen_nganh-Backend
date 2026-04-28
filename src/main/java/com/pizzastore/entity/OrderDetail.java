package com.pizzastore.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "order_details")
public class OrderDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "variant_id")
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
    private List<Topping> toppings = new ArrayList<>();

    @OneToMany(mappedBy = "orderDetail", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<InventoryBatchConsumption> batchConsumptions = new ArrayList<>();

    public OrderDetail() {}


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public List<Topping> getToppings() { return toppings; }
    public void setToppings(List<Topping> toppings) { this.toppings = toppings; }

    public List<InventoryBatchConsumption> getBatchConsumptions() { return batchConsumptions; }
    public void setBatchConsumptions(List<InventoryBatchConsumption> batchConsumptions) { this.batchConsumptions = batchConsumptions; }

    public void addBatchConsumption(InventoryBatchConsumption consumption) {
        batchConsumptions.add(consumption);
        consumption.setOrderDetail(this);
    }

    public void setUnitPrice(Double unitPrice) {
        this.unitPrice = unitPrice;
    }
    public Double getUnitPrice() {
        return unitPrice;
    }
}
