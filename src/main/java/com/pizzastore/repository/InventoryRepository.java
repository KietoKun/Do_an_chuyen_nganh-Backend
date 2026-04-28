package com.pizzastore.repository;

import com.pizzastore.entity.Branch;
import com.pizzastore.entity.Inventory;
import com.pizzastore.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    // Tìm kho của 1 nguyên liệu tại 1 chi nhánh cụ thể
    Optional<Inventory> findByBranchAndProduct(Branch branch, Product product);
    List<Inventory> findByBranch(Branch branch);
}