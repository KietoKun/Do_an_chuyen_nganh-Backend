package com.pizzastore.dto;

import com.pizzastore.enums.DeliveryMethod;
import com.pizzastore.enums.OrderRealtimeEventType;
import com.pizzastore.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public class OrderRealtimeEvent {
    private OrderRealtimeEventType event;
    private Long orderId;
    private OrderStatus status;
    private OrderStatus previousStatus;
    private LocalDateTime orderTime;
    private LocalDateTime acceptedAt;
    private LocalDateTime cookingStartedAt;
    private LocalDateTime completedAt;
    private Double totalPrice;
    private Double discountAmount;
    private Double finalTotalPrice;
    private DeliveryMethod deliveryMethod;
    private String deliveryAddress;
    private String note;
    private Long customerId;
    private String customerName;
    private String customerUsername;
    private Long branchId;
    private String branchName;
    private Long handledById;
    private String handledByName;
    private String handledByUsername;
    private Long cookedById;
    private String cookedByName;
    private String cookedByUsername;
    private String message;
    private List<OrderItem> items;

    public static class OrderItem {
        private Long orderDetailId;
        private Long dishVariantId;
        private String dishName;
        private String size;
        private Integer quantity;
        private Double unitPrice;
        private Double subTotal;
        private List<String> toppings;

        public Long getOrderDetailId() {
            return orderDetailId;
        }

        public void setOrderDetailId(Long orderDetailId) {
            this.orderDetailId = orderDetailId;
        }

        public Long getDishVariantId() {
            return dishVariantId;
        }

        public void setDishVariantId(Long dishVariantId) {
            this.dishVariantId = dishVariantId;
        }

        public String getDishName() {
            return dishName;
        }

        public void setDishName(String dishName) {
            this.dishName = dishName;
        }

        public String getSize() {
            return size;
        }

        public void setSize(String size) {
            this.size = size;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public Double getUnitPrice() {
            return unitPrice;
        }

        public void setUnitPrice(Double unitPrice) {
            this.unitPrice = unitPrice;
        }

        public Double getSubTotal() {
            return subTotal;
        }

        public void setSubTotal(Double subTotal) {
            this.subTotal = subTotal;
        }

        public List<String> getToppings() {
            return toppings;
        }

        public void setToppings(List<String> toppings) {
            this.toppings = toppings;
        }
    }

    public OrderRealtimeEventType getEvent() {
        return event;
    }

    public void setEvent(OrderRealtimeEventType event) {
        this.event = event;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public OrderStatus getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(OrderStatus previousStatus) {
        this.previousStatus = previousStatus;
    }

    public LocalDateTime getOrderTime() {
        return orderTime;
    }

    public void setOrderTime(LocalDateTime orderTime) {
        this.orderTime = orderTime;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(LocalDateTime acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public LocalDateTime getCookingStartedAt() {
        return cookingStartedAt;
    }

    public void setCookingStartedAt(LocalDateTime cookingStartedAt) {
        this.cookingStartedAt = cookingStartedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public Double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(Double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public Double getFinalTotalPrice() {
        return finalTotalPrice;
    }

    public void setFinalTotalPrice(Double finalTotalPrice) {
        this.finalTotalPrice = finalTotalPrice;
    }

    public DeliveryMethod getDeliveryMethod() {
        return deliveryMethod;
    }

    public void setDeliveryMethod(DeliveryMethod deliveryMethod) {
        this.deliveryMethod = deliveryMethod;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerUsername() {
        return customerUsername;
    }

    public void setCustomerUsername(String customerUsername) {
        this.customerUsername = customerUsername;
    }

    public Long getBranchId() {
        return branchId;
    }

    public void setBranchId(Long branchId) {
        this.branchId = branchId;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public Long getHandledById() {
        return handledById;
    }

    public void setHandledById(Long handledById) {
        this.handledById = handledById;
    }

    public String getHandledByName() {
        return handledByName;
    }

    public void setHandledByName(String handledByName) {
        this.handledByName = handledByName;
    }

    public String getHandledByUsername() {
        return handledByUsername;
    }

    public void setHandledByUsername(String handledByUsername) {
        this.handledByUsername = handledByUsername;
    }

    public Long getCookedById() {
        return cookedById;
    }

    public void setCookedById(Long cookedById) {
        this.cookedById = cookedById;
    }

    public String getCookedByName() {
        return cookedByName;
    }

    public void setCookedByName(String cookedByName) {
        this.cookedByName = cookedByName;
    }

    public String getCookedByUsername() {
        return cookedByUsername;
    }

    public void setCookedByUsername(String cookedByUsername) {
        this.cookedByUsername = cookedByUsername;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }
}
