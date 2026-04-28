package com.pizzastore.service;

public interface EmailService {
    boolean sendRegistrationOtpEmail(String recipientEmail, String fullName, String otpCode, long expiresInMinutes);

    boolean sendRegistrationSuccessEmail(String recipientEmail, String fullName, String phoneNumber);

    boolean sendInventoryExpiryAlertEmail(String recipientEmail, String subject, String content);
}
