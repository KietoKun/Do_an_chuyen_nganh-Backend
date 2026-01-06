package com.pizzastore.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;

public class OrderRequest {

    @Schema(description = "Ghi chú cho đơn hàng", example = "Không lấy tương ớt, giao hàng giờ hành chính")
    private String note;

    @Schema(description = "Danh sách sản phẩm trong giỏ")
    private List<CartItem> items = new ArrayList<>();

    @Schema(description = "Phương thức nhận hàng", example = "DELIVERY")
    private String deliveryMethod;

    @Schema(description = "Địa chỉ giao hàng (nếu là DELIVERY)", example = "123 Đường Láng, Hà Nội")
    private String deliveryAddress;

    @Schema(description = "Mã giảm giá", example = "SUMMER2024")
    private String couponCode;

    public static class CartItem {

        @Schema(description = "ID của biến thể sản phẩm (Size M/L...)", example = "10")
        private Long variantId;

        @Schema(description = "Số lượng mua", example = "2")
        private int quantity;

        @Schema(description = "Danh sách ID các topping đã chọn", example = "[1, 5, 8]")
        private List<Long> toppingIds = new ArrayList<>();

        public Long getVariantId() { return variantId; }
        public void setVariantId(Long variantId) { this.variantId = variantId; }

        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }

        public List<Long> getToppingIds() { return toppingIds; }
        public void setToppingIds(List<Long> toppingIds) { this.toppingIds = toppingIds; }
    }


    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) { this.items = items; }

    public String getDeliveryMethod() { return deliveryMethod; }
    public void setDeliveryMethod(String deliveryMethod) { this.deliveryMethod = deliveryMethod; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }
}