package com.pizzastore.repository;

import com.pizzastore.entity.DishComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DishCommentRepository extends JpaRepository<DishComment, Long> {
    long countByCustomer_IdAndDish_Id(Long customerId, Long dishId);

    List<DishComment> findByDish_IdOrderByCreatedAtDesc(Long dishId);

    List<DishComment> findAllByOrderByCreatedAtDesc();

    @Query("select c from DishComment c where c.dish.id = :dishId and (c.visible = true or c.visible is null) order by c.createdAt desc")
    List<DishComment> findVisibleByDishIdOrderByCreatedAtDesc(@Param("dishId") Long dishId);
}
