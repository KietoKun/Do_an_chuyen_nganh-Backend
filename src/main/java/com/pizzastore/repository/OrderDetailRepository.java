package com.pizzastore.repository;

import com.pizzastore.dto.TopSellingDishResponse;
import com.pizzastore.entity.OrderDetail;
import com.pizzastore.enums.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {
    @Query("""
            SELECT COUNT(od)
            FROM OrderDetail od
            WHERE od.order.customer.id = :customerId
              AND od.dishVariant.dish.id = :dishId
              AND od.order.status = :requiredStatus
            """)
    long countPurchasedDishByStatus(@Param("customerId") Long customerId,
                                    @Param("dishId") Long dishId,
                                    @Param("requiredStatus") OrderStatus requiredStatus);

    @Query("""
            SELECT new com.pizzastore.dto.TopSellingDishResponse(
                d.id,
                d.name,
                SUM(od.quantity),
                SUM(COALESCE(od.subTotal, od.unitPrice * od.quantity))
            )
            FROM OrderDetail od
            JOIN od.dishVariant v
            JOIN v.dish d
            JOIN od.order o
            WHERE o.status = :completedStatus
              AND o.branch.id = :branchId
              AND o.orderTime >= :fromDate
              AND o.orderTime <= :toDate
            GROUP BY d.id, d.name
            ORDER BY SUM(od.quantity) DESC, SUM(COALESCE(od.subTotal, od.unitPrice * od.quantity)) DESC
            """)
    List<TopSellingDishResponse> findTopSellingDishesByBranch(@Param("branchId") Long branchId,
                                                              @Param("fromDate") LocalDateTime fromDate,
                                                              @Param("toDate") LocalDateTime toDate,
                                                              @Param("completedStatus") OrderStatus completedStatus,
                                                              Pageable pageable);

    @Query("""
            SELECT new com.pizzastore.dto.TopSellingDishResponse(
                d.id,
                d.name,
                SUM(od.quantity),
                SUM(COALESCE(od.subTotal, od.unitPrice * od.quantity))
            )
            FROM OrderDetail od
            JOIN od.dishVariant v
            JOIN v.dish d
            JOIN od.order o
            WHERE o.status = :completedStatus
              AND o.orderTime >= :fromDate
              AND o.orderTime <= :toDate
            GROUP BY d.id, d.name
            ORDER BY SUM(od.quantity) DESC, SUM(COALESCE(od.subTotal, od.unitPrice * od.quantity)) DESC
            """)
    List<TopSellingDishResponse> findTopSellingDishesAllBranches(@Param("fromDate") LocalDateTime fromDate,
                                                                 @Param("toDate") LocalDateTime toDate,
                                                                 @Param("completedStatus") OrderStatus completedStatus,
                                                                 Pageable pageable);
}
