package com.pizzastore.dto;

import java.time.LocalDateTime;

public class CommentResponse {
    private Long id;
    private Long dishId;
    private String dishName;
    private Long customerId;
    private String customerName;
    private String content;
    private Integer rating;
    private Boolean visible;
    private LocalDateTime createdAt;

    public CommentResponse(Long id, Long dishId, String dishName, Long customerId, String customerName,
                           String content, Integer rating, Boolean visible, LocalDateTime createdAt) {
        this.id = id;
        this.dishId = dishId;
        this.dishName = dishName;
        this.customerId = customerId;
        this.customerName = customerName;
        this.content = content;
        this.rating = rating;
        this.visible = visible;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getDishId() { return dishId; }
    public String getDishName() { return dishName; }
    public Long getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public String getContent() { return content; }
    public Integer getRating() { return rating; }
    public Boolean getVisible() { return visible; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
