package com.pizzastore.service.unit;

import com.pizzastore.dto.ResetPasswordRequest;
import com.pizzastore.dto.VerifyPasswordResetOtpRequest;
import com.pizzastore.entity.Account;
import com.pizzastore.entity.PasswordResetOtp;
import com.pizzastore.enums.RoleName;
import com.pizzastore.repository.AccountRepository;
import com.pizzastore.repository.CustomerRepository;
import com.pizzastore.repository.EmployeeRepository;
import com.pizzastore.repository.PasswordResetOtpRepository;
import com.pizzastore.service.EmailService;
import com.pizzastore.service.PasswordResetService;
import com.pizzastore.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private PasswordResetOtpRepository passwordResetOtpRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(passwordResetService, "otpExpirationMinutes", 5L);
        ReflectionTestUtils.setField(passwordResetService, "otpResendCooldownSeconds", 60L);
    }

    @Test
    void verifyResetOtpShouldReturnTokenAndReplaceOtpHashWithResetTokenHash() {
        Account account = account("0901234567");
        PasswordResetOtp otp = otp("0901234567", "OTP:encoded-otp");
        VerifyPasswordResetOtpRequest request = new VerifyPasswordResetOtpRequest();
        request.setUsername("0901234567");
        request.setOtpCode("123456");

        when(accountRepository.findByUsername("0901234567")).thenReturn(Optional.of(account));
        when(passwordResetOtpRepository.findByUsername("0901234567")).thenReturn(Optional.of(otp));
        when(passwordEncoder.matches("123456", "encoded-otp")).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-token");

        String resetToken = passwordResetService.verifyResetOtp(request);

        assertFalse(resetToken.isBlank());
        ArgumentCaptor<PasswordResetOtp> otpCaptor = ArgumentCaptor.forClass(PasswordResetOtp.class);
        verify(passwordResetOtpRepository).save(otpCaptor.capture());
        assertTrue(otpCaptor.getValue().getOtpHash().startsWith("RESET:"));
    }

    @Test
    void resetPasswordShouldRejectRawOtpBeforeOtpWasVerified() {
        Account account = account("0901234567");
        PasswordResetOtp otp = otp("0901234567", "OTP:encoded-otp");
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setUsername("0901234567");
        request.setResetToken("123456");
        request.setNewPassword("NewPassword@123");

        when(accountRepository.findByUsername("0901234567")).thenReturn(Optional.of(account));
        when(passwordResetOtpRepository.findByUsername("0901234567")).thenReturn(Optional.of(otp));

        assertThrows(ResponseStatusException.class, () -> passwordResetService.resetPassword(request));
    }

    @Test
    void resetPasswordShouldUpdatePasswordWhenResetTokenMatches() {
        Account account = account("0901234567");
        PasswordResetOtp otp = otp("0901234567", "RESET:encoded-token");
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setUsername("0901234567");
        request.setResetToken("reset-token");
        request.setNewPassword("NewPassword@123");

        when(accountRepository.findByUsername("0901234567")).thenReturn(Optional.of(account));
        when(passwordResetOtpRepository.findByUsername("0901234567")).thenReturn(Optional.of(otp));
        when(passwordEncoder.matches("reset-token", "encoded-token")).thenReturn(true);
        when(passwordEncoder.matches("NewPassword@123", "old-password")).thenReturn(false);
        when(passwordEncoder.encode("NewPassword@123")).thenReturn("encoded-new-password");

        passwordResetService.resetPassword(request);

        assertTrue(otp.isUsed());
        assertTrue("encoded-new-password".equals(account.getPassword()));
        verify(accountRepository).save(account);
        verify(passwordResetOtpRepository).save(otp);
        verify(refreshTokenService).deleteByAccount(account);
    }

    private Account account(String username) {
        Account account = new Account();
        account.setUsername(username);
        account.setPassword("old-password");
        account.setRole(RoleName.CUSTOMER);
        return account;
    }

    private PasswordResetOtp otp(String username, String hash) {
        PasswordResetOtp otp = new PasswordResetOtp();
        otp.setUsername(username);
        otp.setEmail("customer@example.com");
        otp.setOtpHash(hash);
        otp.setLastSentAt(LocalDateTime.now());
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        otp.setUsed(false);
        otp.setCreatedAt(LocalDateTime.now());
        return otp;
    }
}
