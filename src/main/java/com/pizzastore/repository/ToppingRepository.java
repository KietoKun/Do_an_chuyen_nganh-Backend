package com.pizzastore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.pizzastore.entity.Topping;
import org.springframework.stereotype.Repository;
@Repository
public interface ToppingRepository extends JpaRepository<Topping, Long> {
}
