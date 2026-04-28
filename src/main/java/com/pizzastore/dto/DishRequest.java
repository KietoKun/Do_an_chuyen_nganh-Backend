package com.pizzastore.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public class DishRequest {

    private String name;
    private String description;
    private String imageUrl;
    private Long categoryId;
    private List<VariantRequest> variants;

    // --- CÁC CLASS CON BÊN TRONG ---
    public static class VariantRequest {
        private String size;
        private Double price;
        private List<RecipeRequest> recipes; // Danh sách công thức của size này

        public String getSize() { return size; }
        public void setSize(String size) { this.size = size; }
        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }
        public List<RecipeRequest> getRecipes() { return recipes; }
        public void setRecipes(List<RecipeRequest> recipes) { this.recipes = recipes; }
    }

    public static class RecipeRequest {
        @Schema(description = "ID của nguyên liệu trong bảng Product", example = "1")
        private Long productId;

        @Schema(description = "Định mức tiêu hao (VD: 0.2 kg)", example = "0.2")
        private Double quantityNeeded;

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public Double getQuantityNeeded() { return quantityNeeded; }
        public void setQuantityNeeded(Double quantityNeeded) { this.quantityNeeded = quantityNeeded; }
    }

    // --- GETTER / SETTER CỦA DISH REQUEST ---
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public List<VariantRequest> getVariants() { return variants; }
    public void setVariants(List<VariantRequest> variants) { this.variants = variants; }
}