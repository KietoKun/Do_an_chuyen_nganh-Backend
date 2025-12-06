package com.pizzastore.dto;
import java.util.List;

public class OrderRequest {
    private String note;
    private List<CartItem> items;

    public static class CartItem {
        private Long variantId;
        private int quantity;

        // Danh sách ID các topping khách chọn (Ví dụ: [1, 2, 5])
        private List<Long> toppingIds;

        // --- GETTER & SETTER ---

        public Long getVariantId() { return variantId; }
        public void setVariantId(Long variantId) { this.variantId = variantId; }

        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }

        // SỬA LẠI ĐOẠN NÀY CHO KHỚP VỚI BIẾN toppingIds
        public List<Long> getToppingIds() {
            return toppingIds;
        }

        public void setToppingIds(List<Long> toppingIds) {
            this.toppingIds = toppingIds;
        }
    }

    // Getter & Setter cho OrderRequest
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) { this.items = items; }
}