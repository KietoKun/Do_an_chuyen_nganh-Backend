package com.pizzastore.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "recipes")
public class Recipe {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "variant_id")
    @JsonIgnore
    private DishVariant dishVariant;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    private Double quantityNeeded;



    public Recipe() {
    }


    public Recipe(DishVariant dishVariant, Product product, Double quantityNeeded) {
        this.dishVariant = dishVariant;
        this.product = product;
        this.quantityNeeded = quantityNeeded;
    }



    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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