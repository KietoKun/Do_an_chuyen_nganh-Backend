package com.pizzastore.repository;

import com.pizzastore.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    // Tìm khách hàng dựa trên username của tài khoản liên kết
    Optional<Customer> findByAccount_Username(String username);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);
}
