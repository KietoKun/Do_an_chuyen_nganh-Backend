package com.pizzastore.dto;

import com.pizzastore.entity.Order;
import com.pizzastore.entity.OrderDetail;
import com.pizzastore.entity.Topping;
import com.pizzastore.enums.DeliveryMethod;
import com.pizzastore.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public class OrderHistoryResponse {
    private Long id;
    private LocalDateTime orderTime;
    private LocalDateTime acceptedAt;
    private LocalDateTime cookingStartedAt;
    private LocalDateTime completedAt;
    private OrderStatus status;
    private DeliveryMethod deliveryMethod;
    private String deliveryAddress;
    private String note;
    private Double totalPrice;
    private String couponCode;
    private Double discountAmount;
    private Double finalTotalPrice;
    private Long branchId;
    private String branchName;
    private List<OrderItemResponse> items;

    public static OrderHistoryResponse from(Order order) {
        OrderHistoryResponse response = new OrderHistoryResponse();
        response.id = order.getId();
        response.orderTime = order.getOrderTime();
        response.acceptedAt = order.getAcceptedAt();
        response.cookingStartedAt = order.getCookingStartedAt();
        response.completedAt = order.getCompletedAt();
        response.status = order.getStatus();
        response.deliveryMethod = order.getDeliveryMethod();
        response.deliveryAddress = order.getDeliveryAddress();
        response.note = order.getNote();
        response.totalPrice = order.getTotalPrice();
        response.couponCode = order.getCouponCode();
        response.discountAmount = order.getDiscountAmount();
        response.finalTotalPrice = order.getFinalTotalPrice();
        if (order.getBranch() != null) {
            response.branchId = order.getBranch().getId();
            response.branchName = order.getBranch().getName();
        }
        response.items = order.getOrderDetails() == null
                ? List.of()
                : order.getOrderDetails().stream()
                        .map(OrderItemResponse::from)
                        .toList();
        return response;
    }

    public static class OrderItemResponse {
        private Long orderDetailId;
        private Long dishVariantId;
        private String dishName;
        private String size;
        private Integer quantity;
        private Double unitPrice;
        private Double subTotal;
        private List<String> toppings;

        private static OrderItemResponse from(OrderDetail detail) {
            OrderItemResponse item = new OrderItemResponse();
            item.orderDetailId = detail.getId();
            item.quantity = detail.getQuantity();
            item.unitPrice = detail.getUnitPrice();
            item.subTotal = detail.getSubTotal();
            if (detail.getDishVariant() != null) {
                item.dishVariantId = detail.getDishVariant().getId();
                item.size = detail.getDishVariant().getSize();
                if (detail.getDishVariant().getDish() != null) {
                    item.dishName = detail.getDishVariant().getDish().getName();
                }
            }
            item.toppings = detail.getToppings() == null
                    ? List.of()
                    : detail.getToppings().stream()
                            .map(Topping::getName)
                            .toList();
            return item;
        }

        public Long getOrderDetailId() { return orderDetailId; }
        public Long getDishVariantId() { return dishVariantId; }
        public String getDishName() { return dishName; }
        public String getSize() { return size; }
        public Integer getQuantity() { return quantity; }
        public Double getUnitPrice() { return unitPrice; }
        public Double getSubTotal() { return subTotal; }
        public List<String> getToppings() { return toppings; }
    }

    public Long getId() { return id; }
    public LocalDateTime getOrderTime() { return orderTime; }
    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public LocalDateTime getCookingStartedAt() { return cookingStartedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public OrderStatus getStatus() { return status; }
    public DeliveryMethod getDeliveryMethod() { return deliveryMethod; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public String getNote() { return note; }
    public Double getTotalPrice() { return totalPrice; }
    public String getCouponCode() { return couponCode; }
    public Double getDiscountAmount() { return discountAmount; }
    public Double getFinalTotalPrice() { return finalTotalPrice; }
    public Long getBranchId() { return branchId; }
    public String getBranchName() { return branchName; }
    public List<OrderItemResponse> getItems() { return items; }
}
