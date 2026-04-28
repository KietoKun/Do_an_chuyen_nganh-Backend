package com.pizzastore.repository;

import com.pizzastore.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {
    // Có thể thêm các hàm tìm kiếm tùy chỉnh sau nếu cần
}