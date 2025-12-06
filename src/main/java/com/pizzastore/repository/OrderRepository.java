package com.pizzastore.repository;

import com.pizzastore.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // Tìm đơn hàng của một khách cụ thể (Lịch sử mua hàng)
    List<Order> findByCustomer_Id(Long customerId);

    // Tìm theo trạng thái (Ví dụ: Bếp tìm các đơn đang PENDING)
    // List<Order> findByStatus(OrderStatus status);
}