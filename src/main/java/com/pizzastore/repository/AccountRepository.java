package com.pizzastore.repository;

import com.pizzastore.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    // Tìm account bằng username để login
    Optional<Account> findByUsername(String username);

    // Kiểm tra username đã tồn tại chưa (lúc tạo user mới)
    Boolean existsByUsername(String username);
}