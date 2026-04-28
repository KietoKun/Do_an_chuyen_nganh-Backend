package com.pizzastore.service;

import com.pizzastore.entity.Account;
import com.pizzastore.entity.RefreshToken;
import com.pizzastore.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenExpirationMs", 604800000L);
    }

    @Test
    void createOrRotateRefreshTokenShouldReturnSavedToken() {
        Account account = new Account();
        account.setId(1L);
        account.setUsername("0912345678");

        when(refreshTokenRepository.findByAccount(account)).thenReturn(Optional.empty());
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken refreshToken = refreshTokenService.createOrRotateRefreshToken(account);

        assertNotNull(refreshToken.getToken());
        assertEquals(account, refreshToken.getAccount());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void verifyRefreshTokenShouldThrowWhenExpired() {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("expired-token");
        refreshToken.setExpiryDate(LocalDateTime.now().minusMinutes(1));

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(refreshToken));

        assertThrows(ResponseStatusException.class,
                () -> refreshTokenService.verifyRefreshToken("expired-token"));
    }
}
