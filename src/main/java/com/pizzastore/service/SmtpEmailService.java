package com.pizzastore.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class SmtpEmailService implements EmailService {
    private static final Logger log = LoggerFactory.getLogger(SmtpEmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.registration.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${spring.mail.username:}")
    private String senderEmail;

    public SmtpEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public boolean sendRegistrationOtpEmail(String recipientEmail, String fullName, String otpCode, long expiresInMinutes) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            return false;
        }

        if (!mailEnabled) {
            log.info("Mail is disabled. Registration OTP mail skipped for {}", recipientEmail);
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(recipientEmail);
            message.setSubject("Pizza Store - Registration OTP");
            message.setText("Hello " + fullName + ",\n\n"
                    + "Your Pizza Store registration OTP is: " + otpCode + "\n"
                    + "This code is valid for " + expiresInMinutes + " minutes.\n\n"
                    + "If you did not request this code, please ignore this email.");
            mailSender.send(message);
            return true;
        } catch (Exception ex) {
            log.error("Failed to send registration OTP email to {}", recipientEmail, ex);
            return false;
        }
    }

    @Override
    public boolean sendRegistrationSuccessEmail(String recipientEmail, String fullName, String phoneNumber) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            return false;
        }

        if (!mailEnabled) {
            log.info("Mail is disabled. Registration success mail skipped for {}", recipientEmail);
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(recipientEmail);
            message.setSubject("Pizza Store - Account Created Successfully");
            message.setText("Hello " + fullName + ",\n\n"
                    + "Your Pizza Store account has been created successfully.\n"
                    + "Login phone number: " + phoneNumber + "\n\n"
                    + "If this was not you, please contact support immediately.");
            mailSender.send(message);
            return true;
        } catch (Exception ex) {
            log.error("Failed to send registration success email to {}", recipientEmail, ex);
            return false;
        }
    }

    @Override
    public boolean sendInventoryExpiryAlertEmail(String recipientEmail, String subject, String content) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            return false;
        }

        if (!mailEnabled) {
            log.info("Mail is disabled. Inventory expiry alert skipped for {}", recipientEmail);
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(recipientEmail);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
            return true;
        } catch (Exception ex) {
            log.error("Failed to send inventory expiry alert email to {}", recipientEmail, ex);
            return false;
        }
    }
}
