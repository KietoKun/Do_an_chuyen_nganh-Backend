package com.pizzastore.repository;

import com.pizzastore.dto.RevenueSummaryResponse;
import com.pizzastore.entity.Order;
import com.pizzastore.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;


@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // Tìm đơn hàng của một khách cụ thể (Lịch sử mua hàng)
    List<Order> findByCustomer_Id(Long customerId);

    // Tìm theo trạng thái (Ví dụ: Bếp tìm các đơn đang PENDING)
    // List<Order> findByStatus(OrderStatus status);
    List<Order> findByCustomer_Account_UsernameOrderByOrderTimeDesc(String username);
    List<Order> findByBranch_IdOrderByOrderTimeDesc(Long branchId);
    @Query("SELECT COUNT(o) FROM Order o WHERE o.customer.id = :customerId AND o.couponCode = :code AND o.status <> 'CANCELLED'")
    long countUsedByCustomer(@Param("customerId") Long customerId, @Param("code") String code);

    @Query("""
            SELECT new com.pizzastore.dto.RevenueSummaryResponse(
                COUNT(o),
                COALESCE(SUM(COALESCE(o.finalTotalPrice, o.totalPrice)), 0.0)
            )
            FROM Order o
            WHERE o.status = :completedStatus
              AND o.branch.id = :branchId
              AND o.orderTime >= :fromDate
              AND o.orderTime <= :toDate
            """)
    RevenueSummaryResponse getRevenueSummaryByBranch(@Param("branchId") Long branchId,
                                                     @Param("fromDate") LocalDateTime fromDate,
                                                     @Param("toDate") LocalDateTime toDate,
                                                     @Param("completedStatus") OrderStatus completedStatus);

    @Query("""
            SELECT new com.pizzastore.dto.RevenueSummaryResponse(
                COUNT(o),
                COALESCE(SUM(COALESCE(o.finalTotalPrice, o.totalPrice)), 0.0)
            )
            FROM Order o
            WHERE o.status = :completedStatus
              AND o.orderTime >= :fromDate
              AND o.orderTime <= :toDate
            """)
    RevenueSummaryResponse getRevenueSummaryAllBranches(@Param("fromDate") LocalDateTime fromDate,
                                                        @Param("toDate") LocalDateTime toDate,
                                                        @Param("completedStatus") OrderStatus completedStatus);

    @Query(value = """
            SELECT CAST(o.order_time AS date) AS revenue_date,
                   COUNT(*) AS order_count,
                   COALESCE(SUM(COALESCE(o.final_total_price, o.total_price)), 0) AS revenue
            FROM orders o
            WHERE o.status = :completedStatus
              AND o.branch_id = :branchId
              AND o.order_time >= :fromDate
              AND o.order_time <= :toDate
            GROUP BY CAST(o.order_time AS date)
            ORDER BY revenue_date
            """, nativeQuery = true)
    List<Object[]> getDailyRevenueByBranch(@Param("branchId") Long branchId,
                                           @Param("fromDate") LocalDateTime fromDate,
                                           @Param("toDate") LocalDateTime toDate,
                                           @Param("completedStatus") String completedStatus);

    @Query(value = """
            SELECT CAST(o.order_time AS date) AS revenue_date,
                   COUNT(*) AS order_count,
                   COALESCE(SUM(COALESCE(o.final_total_price, o.total_price)), 0) AS revenue
            FROM orders o
            WHERE o.status = :completedStatus
              AND o.order_time >= :fromDate
              AND o.order_time <= :toDate
            GROUP BY CAST(o.order_time AS date)
            ORDER BY revenue_date
            """, nativeQuery = true)
    List<Object[]> getDailyRevenueAllBranches(@Param("fromDate") LocalDateTime fromDate,
                                              @Param("toDate") LocalDateTime toDate,
                                              @Param("completedStatus") String completedStatus);
}
