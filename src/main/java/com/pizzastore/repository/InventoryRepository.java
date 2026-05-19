package com.pizzastore.repository;

import com.pizzastore.entity.Branch;
import com.pizzastore.entity.Inventory;
import com.pizzastore.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByBranchAndProduct(Branch branch, Product product);

    List<Inventory> findByBranch(Branch branch);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT i
            FROM Inventory i
            WHERE i.branch = :branch
              AND i.product IN :products
            ORDER BY i.product.id ASC
            """)
    List<Inventory> findByBranchAndProductsForUpdate(@Param("branch") Branch branch,
                                                     @Param("products") List<Product> products);
}
