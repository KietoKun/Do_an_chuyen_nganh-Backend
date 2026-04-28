package com.pizzastore.dto;

import com.pizzastore.enums.DeliveryMethod;
import com.pizzastore.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public class BranchDetailResponse {
    private Long id;
    private String name;
    private String address;
    private Double latitude;
    private Double longitude;
    private boolean active;
    private List<EmployeeSummary> employees;
    private List<OrderSummary> orders;

    public BranchDetailResponse(Long id, String name, String address, Double latitude, Double longitude, boolean active,
                                List<EmployeeSummary> employees, List<OrderSummary> orders) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.active = active;
        this.employees = employees;
        this.orders = orders;
    }

    public static class EmployeeSummary {
        private Long id;
        private String fullName;
        private String phoneNumber;
        private String position;
        private String role;

        public EmployeeSummary(Long id, String fullName, String phoneNumber, String position, String role) {
            this.id = id;
            this.fullName = fullName;
            this.phoneNumber = phoneNumber;
            this.position = position;
            this.role = role;
        }

        public Long getId() { return id; }
        public String getFullName() { return fullName; }
        public String getPhoneNumber() { return phoneNumber; }
        public String getPosition() { return position; }
        public String getRole() { return role; }
    }

    public static class OrderSummary {
        private Long id;
        private LocalDateTime orderTime;
        private LocalDateTime acceptedAt;
        private LocalDateTime cookingStartedAt;
        private LocalDateTime completedAt;
        private OrderStatus status;
        private DeliveryMethod deliveryMethod;
        private String deliveryAddress;
        private Double finalTotalPrice;
        private String customerName;
        private String handledByName;
        private String cookedByName;

        public OrderSummary(Long id, LocalDateTime orderTime, LocalDateTime acceptedAt, LocalDateTime cookingStartedAt,
                            LocalDateTime completedAt, OrderStatus status, DeliveryMethod deliveryMethod,
                            String deliveryAddress, Double finalTotalPrice, String customerName, String handledByName,
                            String cookedByName) {
            this.id = id;
            this.orderTime = orderTime;
            this.acceptedAt = acceptedAt;
            this.cookingStartedAt = cookingStartedAt;
            this.completedAt = completedAt;
            this.status = status;
            this.deliveryMethod = deliveryMethod;
            this.deliveryAddress = deliveryAddress;
            this.finalTotalPrice = finalTotalPrice;
            this.customerName = customerName;
            this.handledByName = handledByName;
            this.cookedByName = cookedByName;
        }

        public Long getId() { return id; }
        public LocalDateTime getOrderTime() { return orderTime; }
        public LocalDateTime getAcceptedAt() { return acceptedAt; }
        public LocalDateTime getCookingStartedAt() { return cookingStartedAt; }
        public LocalDateTime getCompletedAt() { return completedAt; }
        public OrderStatus getStatus() { return status; }
        public DeliveryMethod getDeliveryMethod() { return deliveryMethod; }
        public String getDeliveryAddress() { return deliveryAddress; }
        public Double getFinalTotalPrice() { return finalTotalPrice; }
        public String getCustomerName() { return customerName; }
        public String getHandledByName() { return handledByName; }
        public String getCookedByName() { return cookedByName; }
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public boolean isActive() { return active; }
    public List<EmployeeSummary> getEmployees() { return employees; }
    public List<OrderSummary> getOrders() { return orders; }
}
