package com.pizzastore.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Thong tin mot mon ban chay trong bao cao top seller")
public class TopSellingDishResponse {
    @Schema(description = "ID mon an", example = "1")
    private Long dishId;

    @Schema(description = "Ten mon an", example = "Pizza Hai San")
    private String dishName;

    @Schema(description = "Tong so luong mon da ban trong khoang loc", example = "50")
    private Long quantitySold;

    @Schema(description = "Doanh thu cua mon, tinh theo tong OrderDetail.subTotal cua cac don COMPLETED", example = "7500000")
    private Double revenue;

    public TopSellingDishResponse(Long dishId, String dishName, Long quantitySold, Double revenue) {
        this.dishId = dishId;
        this.dishName = dishName;
        this.quantitySold = quantitySold;
        this.revenue = revenue;
    }

    public Long getDishId() { return dishId; }
    public String getDishName() { return dishName; }
    public Long getQuantitySold() { return quantitySold; }
    public Double getRevenue() { return revenue; }
}
