package com.pizzastore.repository;

import com.pizzastore.entity.PasswordResetOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {
    Optional<PasswordResetOtp> findByUsername(String username);

    void deleteByUsername(String username);
}
