package com.pizzastore.repository;

import com.pizzastore.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    boolean existsByCode(String code);

    // --- SỬA LẠI QUERY CHO ĐÚNG TÊN BIẾN MỚI ---
    // 1. active = true (Thay vì isActive)
    // 2. expirationDate >= CURRENT_DATE (Thay vì endDate > CURRENT_TIMESTAMP)
    // 3. Thêm điều kiện: usageLimit IS NULL (nếu không giới hạn) hoặc usageCount < limit
    @Query("SELECT c FROM Coupon c WHERE c.active = true AND c.expirationDate >= CURRENT_DATE AND (c.usageLimit IS NULL OR c.usageCount < c.usageLimit)")
    List<Coupon> findValidCoupons();

    // Dùng Optional an toàn hơn để tránh NullPointerException
    Optional<Coupon> findByCode(String code);

    @Modifying
    @Transactional
    @Query("UPDATE Coupon c SET c.active = false WHERE c.active = true AND c.expirationDate < CURRENT_DATE")
    int deactivateExpiredCoupons();
}