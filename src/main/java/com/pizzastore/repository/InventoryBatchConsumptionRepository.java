package com.pizzastore.repository;

import com.pizzastore.entity.InventoryBatchConsumption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryBatchConsumptionRepository extends JpaRepository<InventoryBatchConsumption, Long> {
    List<InventoryBatchConsumption> findByOrderDetail_Order_IdAndReturnedFalse(Long orderId);
}
