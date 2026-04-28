package com.pizzastore.repository;

import com.pizzastore.entity.Branch;
import com.pizzastore.entity.InventoryBatch;
import com.pizzastore.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InventoryBatchRepository extends JpaRepository<InventoryBatch, Long> {
    List<InventoryBatch> findByBranchOrderByProduct_NameAscExpiredAtAscImportedAtAsc(Branch branch);

    @Query("""
            SELECT b
            FROM InventoryBatch b
            WHERE b.branch = :branch
              AND b.product = :product
              AND b.quantityRemaining > 0
              AND (b.expiredAt IS NULL OR b.expiredAt >= :today)
            ORDER BY CASE WHEN b.expiredAt IS NULL THEN 1 ELSE 0 END, b.expiredAt ASC, b.importedAt ASC
            """)
    List<InventoryBatch> findUsableBatchesForDeduction(@Param("branch") Branch branch,
                                                       @Param("product") Product product,
                                                       @Param("today") LocalDate today);

    @Query("""
            SELECT b
            FROM InventoryBatch b
            WHERE b.quantityRemaining > 0
              AND b.expiredAt IS NOT NULL
              AND b.expiredAt BETWEEN :fromDate AND :toDate
            ORDER BY b.branch.name ASC, b.expiredAt ASC, b.product.name ASC, b.importedAt ASC
            """)
    List<InventoryBatch> findExpiringBatches(@Param("fromDate") LocalDate fromDate,
                                             @Param("toDate") LocalDate toDate);
}
