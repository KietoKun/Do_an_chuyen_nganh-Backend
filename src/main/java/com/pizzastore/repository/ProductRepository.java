package com.pizzastore.repository;

import com.pizzastore.entity.Product; // <--- 1. Import Entity
import org.springframework.data.jpa.repository.JpaRepository; // <--- 2. Import JpaRepository
import org.springframework.stereotype.Repository; // <--- 3. Import Annotation

import java.util.List; // (Tùy chọn) Nếu bạn muốn dùng List

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // --- CÁC HÀM TÙY CHỌN (Gợi ý thêm) ---

    // Tìm nguyên liệu theo tên (Ví dụ để check trùng tên khi nhập kho)
    boolean existsByName(String name);

    // Tìm các nguyên liệu sắp hết hàng (Ví dụ: Tồn kho < 10)
    List<Product> findByStockQuantityLessThan(Double quantity);
}