package com.pizzastore.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class ToppingRequest {
    @Schema(description = "Ten topping", example = "Them pho mai")
    private String name;

    @Schema(description = "Gia tien topping", example = "15000")
    private Double price;

    @Schema(description = "ID nguyen lieu bi tru khi dung topping", example = "2")
    private Long productId;

    @Schema(description = "Dinh muc nguyen lieu can dung cho topping", example = "0.05")
    private Double quantityNeeded;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Double getQuantityNeeded() {
        return quantityNeeded;
    }

    public void setQuantityNeeded(Double quantityNeeded) {
        this.quantityNeeded = quantityNeeded;
    }
}
