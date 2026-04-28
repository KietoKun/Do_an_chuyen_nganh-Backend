package com.pizzastore.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Doanh thu va so don hang theo ngay")
public class DailyRevenueResponse {
    @Schema(description = "Ngay thong ke", example = "2026-04-26")
    private LocalDate date;

    @Schema(description = "Tong doanh thu trong ngay", example = "135000")
    private Double revenue;

    @Schema(description = "So don hang COMPLETED trong ngay", example = "1")
    private Long orderCount;

    public DailyRevenueResponse(LocalDate date, Double revenue, Long orderCount) {
        this.date = date;
        this.revenue = revenue;
        this.orderCount = orderCount;
    }

    public LocalDate getDate() {
        return date;
    }

    public Double getRevenue() {
        return revenue;
    }

    public Long getOrderCount() {
        return orderCount;
    }
}
