package com.pizzastore.service;

import com.pizzastore.dto.RegisterOtpRequest;
import com.pizzastore.dto.RegisterRequest;
import com.pizzastore.entity.RegistrationOtp;
import com.pizzastore.repository.AccountRepository;
import com.pizzastore.repository.CustomerRepository;
import com.pizzastore.repository.RegistrationOtpRepository;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private RegistrationOtpRepository registrationOtpRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private RegistrationService registrationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(registrationService, "otpExpirationMinutes", 5L);
        ReflectionTestUtils.setField(registrationService, "otpResendCooldownSeconds", 60L);
    }

    @Test
    void sendRegistrationOtpShouldPersistEncodedOtpAndCallEmailProvider() {
        RegisterOtpRequest request = new RegisterOtpRequest();
        request.setFullName("Nguyen Van A");
        request.setPhoneNumber("0912345678");
        request.setEmail("a@example.com");
        request.setAddress("HCM");

        when(accountRepository.existsByUsername(request.getPhoneNumber())).thenReturn(false);
        when(registrationOtpRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-otp");
        when(emailService.sendRegistrationOtpEmail(eq(request.getEmail()), eq(request.getFullName()), anyString(), anyLong()))
                .thenReturn(true);

        registrationService.sendRegistrationOtp(request);

        verify(emailService).sendRegistrationOtpEmail(eq(request.getEmail()), eq(request.getFullName()), anyString(), anyLong());

        ArgumentCaptor<RegistrationOtp> otpCaptor = ArgumentCaptor.forClass(RegistrationOtp.class);
        verify(registrationOtpRepository).save(otpCaptor.capture());
        RegistrationOtp savedOtp = otpCaptor.getValue();

        assertTrue("encoded-otp".equals(savedOtp.getOtpHash()));
        assertTrue(savedOtp.getExpiresAt().isAfter(LocalDateTime.now()));
        assertTrue(request.getEmail().equals(savedOtp.getEmail()));
        assertTrue(request.getPhoneNumber().equals(savedOtp.getPhoneNumber()));
    }

    @Test
    void registerCustomerShouldCreateAccountWhenOtpMatches() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Nguyen Van B");
        request.setPhoneNumber("0988888888");
        request.setEmail("b@example.com");
        request.setAddress("Ha Noi");
        request.setPassword("secret");
        request.setOtpCode("123456");

        RegistrationOtp otp = new RegistrationOtp();
        otp.setEmail(request.getEmail());
        otp.setPhoneNumber(request.getPhoneNumber());
        otp.setOtpHash("encoded-otp");
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        when(accountRepository.existsByUsername(request.getPhoneNumber())).thenReturn(false);
        when(registrationOtpRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(otp));
        when(passwordEncoder.matches(request.getOtpCode(), otp.getOtpHash())).thenReturn(true);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-password");
        when(emailService.sendRegistrationSuccessEmail(request.getEmail(), request.getFullName(), request.getPhoneNumber()))
                .thenReturn(true);

        boolean emailSent = registrationService.registerCustomer(request);

        assertTrue(emailSent);
        verify(customerRepository).save(any());
        verify(registrationOtpRepository).delete(otp);
    }

    @Test
    void registerCustomerShouldRejectWhenOtpIsMissingForEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Nguyen Van C");
        request.setPhoneNumber("0977777777");
        request.setEmail("other@example.com");
        request.setAddress("Da Nang");
        request.setPassword("secret");
        request.setOtpCode("123456");

        when(accountRepository.existsByUsername(request.getPhoneNumber())).thenReturn(false);
        when(registrationOtpRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> registrationService.registerCustomer(request));

        assertFalse(ex.getReason() == null || ex.getReason().isBlank());
        verify(customerRepository, never()).save(any());
        verify(emailService, never()).sendRegistrationSuccessEmail(anyString(), anyString(), anyString());
    }
}
