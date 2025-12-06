package com.pizzastore.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dishes")
public class Dish {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    private String imageUrl;

    private boolean isAvailable = true;

    // --- 1. THÊM QUAN HỆ VỚI CATEGORY ---
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    // --- 2. THÊM QUAN HỆ VỚI DISH_VARIANT ---
    // (Thay thế cho price và recipes cũ)
    // Khi lưu Dish, tự động lưu các size (S, M, L) đi kèm
    @OneToMany(mappedBy = "dish", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DishVariant> variants = new ArrayList<>();

    // --- CONSTRUCTORS ---
    public Dish() {}

    // --- GETTERS & SETTERS ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public List<DishVariant> getVariants() { return variants; }
    public void setVariants(List<DishVariant> variants) { this.variants = variants; }

    // --- HELPER METHODS (Tiện ích) ---

    // Hàm thêm nhanh một biến thể (Size)
    public void addVariant(DishVariant variant) {
        variants.add(variant);
        variant.setDish(this);
    }
}