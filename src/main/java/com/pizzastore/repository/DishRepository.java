package com.pizzastore.repository;

import com.pizzastore.entity.Dish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DishRepository extends JpaRepository<Dish, Long> {
    // Tìm tất cả món đang "Còn hàng" (isAvailable = true)
    List<Dish> findByIsAvailableTrue();
}