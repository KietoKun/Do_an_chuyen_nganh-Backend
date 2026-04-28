package com.pizzastore.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tong quan doanh thu trong khoang loc")
public class RevenueSummaryResponse {
    @Schema(description = "So don hang COMPLETED duoc tinh vao doanh thu", example = "20")
    private Long orderCount;

    @Schema(description = "Tong doanh thu sau giam gia, tinh theo finalTotalPrice neu co, nguoc lai dung totalPrice", example = "12000000")
    private Double revenue;

    public RevenueSummaryResponse(Long orderCount, Double revenue) {
        this.orderCount = orderCount;
        this.revenue = revenue;
    }

    public Long getOrderCount() { return orderCount; }
    public Double getRevenue() { return revenue; }
}
