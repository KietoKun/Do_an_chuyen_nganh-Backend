package com.pizzastore.repository;

import com.pizzastore.entity.DishVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DishVariantRepository extends JpaRepository<DishVariant, Long> {
}