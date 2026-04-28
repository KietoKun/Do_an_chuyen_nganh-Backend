package com.pizzastore.repository;

import com.pizzastore.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Tìm nguyên liệu theo tên (Ví dụ để check trùng tên khi thêm nguyên liệu mới)
    boolean existsByName(String name);

    // ĐÃ XÓA/COMMENT HÀM DƯỚI ĐÂY VÌ BẢNG PRODUCT KHÔNG CÒN CỘT STOCK_QUANTITY NỮA
    // List<Product> findByStockQuantityLessThan(Double quantity);

    Product findByName(String name);
}