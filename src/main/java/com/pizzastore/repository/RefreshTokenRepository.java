package com.pizzastore.repository;

import com.pizzastore.entity.Account;
import com.pizzastore.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findByAccount(Account account);

    void deleteByAccount(Account account);
}
