package com.pizzastore.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_batches",
        indexes = {
                @Index(name = "idx_inventory_batches_branch_product_expired", columnList = "branch_id,product_id,expired_at"),
                @Index(name = "idx_inventory_batches_remaining", columnList = "quantity_remaining")
        })
public class InventoryBatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity_imported", nullable = false)
    private Double quantityImported;

    @Column(name = "quantity_remaining", nullable = false)
    private Double quantityRemaining;

    @Column(name = "imported_at", nullable = false)
    private LocalDateTime importedAt;

    @Column(name = "expired_at")
    private LocalDate expiredAt;

    public InventoryBatch() {}

    public InventoryBatch(Branch branch, Product product, Double quantityImported, LocalDateTime importedAt, LocalDate expiredAt) {
        this.branch = branch;
        this.product = product;
        this.quantityImported = quantityImported;
        this.quantityRemaining = quantityImported;
        this.importedAt = importedAt;
        this.expiredAt = expiredAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Branch getBranch() { return branch; }
    public void setBranch(Branch branch) { this.branch = branch; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public Double getQuantityImported() { return quantityImported; }
    public void setQuantityImported(Double quantityImported) { this.quantityImported = quantityImported; }

    public Double getQuantityRemaining() { return quantityRemaining; }
    public void setQuantityRemaining(Double quantityRemaining) { this.quantityRemaining = quantityRemaining; }

    public LocalDateTime getImportedAt() { return importedAt; }
    public void setImportedAt(LocalDateTime importedAt) { this.importedAt = importedAt; }

    public LocalDate getExpiredAt() { return expiredAt; }
    public void setExpiredAt(LocalDate expiredAt) { this.expiredAt = expiredAt; }
}
