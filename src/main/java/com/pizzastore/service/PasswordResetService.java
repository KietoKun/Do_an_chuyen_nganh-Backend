package com.pizzastore.service;

import com.pizzastore.dto.ForgotPasswordRequest;
import com.pizzastore.dto.ResetPasswordRequest;
import com.pizzastore.entity.Account;
import com.pizzastore.entity.Customer;
import com.pizzastore.entity.Employee;
import com.pizzastore.entity.PasswordResetOtp;
import com.pizzastore.enums.RoleName;
import com.pizzastore.repository.AccountRepository;
import com.pizzastore.repository.CustomerRepository;
import com.pizzastore.repository.EmployeeRepository;
import com.pizzastore.repository.PasswordResetOtpRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class PasswordResetService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordResetOtpRepository passwordResetOtpRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.password-reset.otp.expiration-minutes:5}")
    private long otpExpirationMinutes;

    @Value("${app.password-reset.otp.resend-cooldown-seconds:60}")
    private long otpResendCooldownSeconds;

    public PasswordResetService(AccountRepository accountRepository,
                                CustomerRepository customerRepository,
                                EmployeeRepository employeeRepository,
                                PasswordResetOtpRepository passwordResetOtpRepository,
                                PasswordEncoder passwordEncoder,
                                EmailService emailService,
                                RefreshTokenService refreshTokenService) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
        this.passwordResetOtpRepository = passwordResetOtpRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public void requestResetOtp(ForgotPasswordRequest request) {
        String username = normalizeUsername(request == null ? null : request.getUsername());
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tài khoản không tồn tại."));

        ResetRecipient recipient = resolveRecipient(account);
        passwordResetOtpRepository.findByUsername(username)
                .ifPresent(existing -> {
                    if (existing.getLastSentAt() != null
                            && existing.getLastSentAt().plusSeconds(otpResendCooldownSeconds).isAfter(LocalDateTime.now())) {
                        throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                                "Vui lòng đợi trước khi yêu cầu mã xác thực mới.");
                    }
                });

        String otpCode = generateOtpCode();
        boolean sent = emailService.sendPasswordResetOtpEmail(
                recipient.email(),
                recipient.fullName(),
                otpCode,
                otpExpirationMinutes
        );
        if (!sent) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Không gửi được mã xác thực đến email của tài khoản này.");
        }

        PasswordResetOtp otp = passwordResetOtpRepository.findByUsername(username)
                .orElseGet(PasswordResetOtp::new);
        otp.setUsername(username);
        otp.setEmail(recipient.email());
        otp.setOtpHash(passwordEncoder.encode(otpCode));
        otp.setLastSentAt(LocalDateTime.now());
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes));
        otp.setCreatedAt(otp.getCreatedAt() == null ? LocalDateTime.now() : otp.getCreatedAt());
        otp.setUsed(false);
        passwordResetOtpRepository.save(otp);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String username = normalizeUsername(request == null ? null : request.getUsername());
        String otpCode = requireNotBlank(request == null ? null : request.getOtpCode(), "Vui lòng nhập mã xác thực.");
        String newPassword = requireNotBlank(request == null ? null : request.getNewPassword(), "Vui lòng nhập mật khẩu mới.");

        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tài khoản không tồn tại."));

        PasswordResetOtp otp = passwordResetOtpRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Bạn cần yêu cầu mã xác thực trước khi đặt lại mật khẩu."));

        if (otp.isUsed()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã xác thực đã được sử dụng.");
        }
        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            passwordResetOtpRepository.delete(otp);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã xác thực đã hết hạn.");
        }
        if (!passwordEncoder.matches(otpCode, otp.getOtpHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã xác thực không chính xác.");
        }
        if (passwordEncoder.matches(newPassword, account.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Mật khẩu mới không được trùng với mật khẩu cũ.");
        }

        account.setPassword(passwordEncoder.encode(newPassword));
        account.setFirstLogin(false);
        accountRepository.save(account);

        otp.setUsed(true);
        passwordResetOtpRepository.save(otp);
        refreshTokenService.deleteByAccount(account);
    }

    private ResetRecipient resolveRecipient(Account account) {
        if (account.getRole() == RoleName.CUSTOMER) {
            Customer customer = customerRepository.findByAccount_Username(account.getUsername())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Không tìm thấy hồ sơ khách hàng của tài khoản này."));
            return new ResetRecipient(
                    requireNotBlank(customer.getEmail(), "Tài khoản này chưa có email để nhận mã xác thực."),
                    defaultName(customer.getFullName())
            );
        }

        Employee employee = employeeRepository.findByAccount_Username(account.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Không tìm thấy hồ sơ nhân viên của tài khoản này."));
        return new ResetRecipient(
                requireNotBlank(employee.getEmail(), "Tài khoản này chưa có email để nhận mã xác thực."),
                defaultName(employee.getFullName())
        );
    }

    private String normalizeUsername(String username) {
        return requireNotBlank(username, "Vui lòng nhập số điện thoại hoặc tên đăng nhập.");
    }

    private String requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private String defaultName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "bạn";
        }
        return fullName.trim();
    }

    private String generateOtpCode() {
        int otp = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(otp);
    }

    private record ResetRecipient(String email, String fullName) {
    }
}
