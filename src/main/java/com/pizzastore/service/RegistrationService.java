package com.pizzastore.service;

import com.pizzastore.dto.RegisterOtpRequest;
import com.pizzastore.dto.RegisterRequest;
import com.pizzastore.entity.Account;
import com.pizzastore.entity.Customer;
import com.pizzastore.entity.RegistrationOtp;
import com.pizzastore.enums.RoleName;
import com.pizzastore.repository.AccountRepository;
import com.pizzastore.repository.CustomerRepository;
import com.pizzastore.repository.RegistrationOtpRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class RegistrationService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final RegistrationOtpRepository registrationOtpRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.registration.otp.expiration-minutes:5}")
    private long otpExpirationMinutes;

    @Value("${app.registration.otp.resend-cooldown-seconds:60}")
    private long otpResendCooldownSeconds;

    public RegistrationService(AccountRepository accountRepository,
                               CustomerRepository customerRepository,
                               RegistrationOtpRepository registrationOtpRepository,
                               PasswordEncoder passwordEncoder,
                               EmailService emailService) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.registrationOtpRepository = registrationOtpRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public void sendRegistrationOtp(RegisterOtpRequest request) {
        validateOtpRequest(request);

        if (accountRepository.existsByUsername(request.getPhoneNumber())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điện thoại này đã được đăng ký");
        }
        if (customerRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điện thoại này đã được đăng ký");
        }
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email này đã được đăng ký");
        }

        registrationOtpRepository.findByEmail(request.getEmail())
                .ifPresent(existing -> {
                    if (existing.getLastSentAt() != null
                            && existing.getLastSentAt().plusSeconds(otpResendCooldownSeconds).isAfter(LocalDateTime.now())) {
                        throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                                "Vui lòng đợi trước khi yêu cầu mã mới.");
                    }
                });

        String otpCode = generateOtpCode();
        boolean sent = emailService.sendRegistrationOtpEmail(
                request.getEmail(),
                request.getFullName(),
                otpCode,
                otpExpirationMinutes
        );
        if (!sent) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Không gửi được mã xác thực đến email này.");
        }

        RegistrationOtp otp = registrationOtpRepository.findByEmail(request.getEmail())
                .orElseGet(RegistrationOtp::new);
        otp.setEmail(request.getEmail());
        otp.setPhoneNumber(request.getPhoneNumber());
        otp.setOtpHash(passwordEncoder.encode(otpCode));
        otp.setLastSentAt(LocalDateTime.now());
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes));
        registrationOtpRepository.save(otp);
    }

    @Transactional
    public boolean registerCustomer(RegisterRequest request) {
        validateRegisterRequest(request);

        if (request.getOtpCode() == null || request.getOtpCode().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng nhập mã xác thực OTP");
        }

        if (accountRepository.existsByUsername(request.getPhoneNumber())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điện thoại này đã được đăng ký");
        }
        if (customerRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điện thoại này đã được đăng ký");
        }
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email này đã được đăng ký");
        }

        RegistrationOtp otp = registrationOtpRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Bạn cần gửi mã OTP qua email trước khi đăng ký"));

        if (!request.getPhoneNumber().equals(otp.getPhoneNumber())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Số điện thoại không khớp với yêu cầu OTP");
        }

        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            registrationOtpRepository.delete(otp);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã OTP đã hết hạn");
        }

        if (!passwordEncoder.matches(request.getOtpCode(), otp.getOtpHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mã OTP không chính xác");
        }

        Account account = new Account();
        account.setUsername(request.getPhoneNumber());
        account.setPassword(passwordEncoder.encode(request.getPassword()));
        account.setRole(RoleName.CUSTOMER);
        account.setFirstLogin(false);

        Customer customer = new Customer();
        customer.setFullName(request.getFullName());
        customer.setPhoneNumber(request.getPhoneNumber());
        customer.setAddress(request.getAddress());
        customer.setEmail(request.getEmail());
        customer.setAccount(account);

        customerRepository.save(customer);
        registrationOtpRepository.delete(otp);

        return emailService.sendRegistrationSuccessEmail(
                request.getEmail(),
                request.getFullName(),
                request.getPhoneNumber()
        );
    }

    private void validateOtpRequest(RegisterOtpRequest request) {
        validateCommonRegistrationFields(request.getFullName(), request.getPhoneNumber(), request.getEmail());
    }

    private void validateRegisterRequest(RegisterRequest request) {
        validateCommonRegistrationFields(request.getFullName(), request.getPhoneNumber(), request.getEmail());
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu không được để trống");
        }
    }

    private void validateCommonRegistrationFields(String fullName, String phoneNumber, String email) {
        if (fullName == null || fullName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Họ tên không được để trống");
        }
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điện thoại không được để trống");
        }
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email không được để trống");
        }
    }

    private String generateOtpCode() {
        int otp = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(otp);
    }
}
