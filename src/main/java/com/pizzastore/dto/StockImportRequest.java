package com.pizzastore.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

public class StockImportRequest {

    @Schema(description = "ID của Chi nhánh nhận hàng", example = "1")
    private Long branchId;

    @Schema(description = "ID của Nguyên liệu", example = "1")
    private Long productId;

    @Schema(description = "Số lượng nhập thêm vào kho", example = "10.5")
    private Double quantityAdded;

    @Schema(description = "Ngay nhap kho, dinh dang yyyy-MM-dd. Neu bo trong, he thong lay ngay hien tai.", example = "2026-04-22")
    private LocalDate importedDate;

    @Schema(description = "Ngay het han cua lo nguyen lieu, dinh dang yyyy-MM-dd. Co the bo trong neu nguyen lieu khong co han.", example = "2026-05-22")
    private LocalDate expiredAt;

    public Long getBranchId() { return branchId; }
    public void setBranchId(Long branchId) { this.branchId = branchId; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Double getQuantityAdded() { return quantityAdded; }
    public void setQuantityAdded(Double quantityAdded) { this.quantityAdded = quantityAdded; }

    public LocalDate getImportedDate() { return importedDate; }
    public void setImportedDate(LocalDate importedDate) { this.importedDate = importedDate; }

    public LocalDate getExpiredAt() { return expiredAt; }
    public void setExpiredAt(LocalDate expiredAt) { this.expiredAt = expiredAt; }
}
