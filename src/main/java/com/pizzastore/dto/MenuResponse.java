package com.pizzastore.dto;

import java.util.List;

public class MenuResponse {
    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private String category;
    private List<VariantDto> variants;

    // --- CONSTRUCTOR ---
    public MenuResponse(Long id, String name, String description, String imageUrl, String category, List<VariantDto> variants) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.category = category;
        this.variants = variants;
    }

    // --- GETTERS & SETTERS (Lớp Cha) ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<VariantDto> getVariants() {
        return variants;
    }

    public void setVariants(List<VariantDto> variants) {
        this.variants = variants;
    }

    // ==========================================
    // INNER CLASS: VARIANT DTO
    // ==========================================
    public static class VariantDto {
        private Long id;
        private String size;
        private Double price;
        private int maxQuantity;

        public VariantDto(Long id, String size, Double price, int maxQuantity) {
            this.id = id;
            this.size = size;
            this.price = price;
            this.maxQuantity = maxQuantity;
        }

        // --- GETTERS & SETTERS (Lớp Con) ---

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getSize() {
            return size;
        }

        public void setSize(String size) {
            this.size = size;
        }

        public Double getPrice() {
            return price;
        }

        public void setPrice(Double price) {
            this.price = price;
        }

        public int getMaxQuantity() {
            return maxQuantity;
        }

        public void setMaxQuantity(int maxQuantity) {
            this.maxQuantity = maxQuantity;
        }
    }
}