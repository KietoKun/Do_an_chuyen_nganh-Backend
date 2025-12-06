package com.pizzastore.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dish_variants")
public class DishVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String size;  // S, M, L, XL, Standard
    private Double price; // Giá tiền riêng cho size này

    @ManyToOne
    @JoinColumn(name = "dish_id")
    @JsonIgnore // Ngắt vòng lặp JSON khi in DishVariant
    private Dish dish;

    // Quan hệ: Một size có danh sách công thức riêng
    @OneToMany(mappedBy = "dishVariant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Recipe> recipes = new ArrayList<>();

    // ==========================================
    // 1. CONSTRUCTORS
    // ==========================================
    public DishVariant() {}

    public DishVariant(String size, Double price, Dish dish) {
        this.size = size;
        this.price = price;
        this.dish = dish;
    }

    // ==========================================
    // 2. HELPER METHOD (Rất quan trọng)
    // ==========================================
    // Hàm này giúp thêm Recipe vào Variant và tự động gán ngược lại
    // Giúp code Seeder ngắn gọn hơn, không bị quên setDishVariant
    public void addRecipe(Recipe recipe) {
        this.recipes.add(recipe);
        recipe.setDishVariant(this);
    }

    // ==========================================
    // 3. GETTERS & SETTERS (Bắt buộc phải có đủ)
    // ==========================================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Dish getDish() { return dish; }
    public void setDish(Dish dish) { this.dish = dish; }

    public List<Recipe> getRecipes() { return recipes; }
    public void setRecipes(List<Recipe> recipes) { this.recipes = recipes; }
}