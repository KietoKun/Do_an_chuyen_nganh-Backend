package com.pizzastore.repository;

import com.pizzastore.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    // Tìm thanh toán theo mã đơn hàng
    Optional<Payment> findByOrder_Id(Long orderId);

    // Tìm theo mã giao dịch VNPAY (để xử lý callback)
    Optional<Payment> findByTransactionCode(String transactionCode);
}