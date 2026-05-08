package com.pizzastore.service.integration;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.pizzastore.service.SmtpEmailService;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.InternetAddress;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {SmtpEmailService.class, SmtpEmailServiceIntegrationTest.MailTestConfig.class})
@TestPropertySource(properties = {
        "app.registration.mail.enabled=true",
        "spring.mail.host=localhost",
        "spring.mail.port=3025",
        "spring.mail.username=test-sender@example.com",
        "spring.mail.password=test-password"
})
class SmtpEmailServiceIntegrationTest {

    private static GreenMail greenMail;

    @Autowired
    private SmtpEmailService smtpEmailService;

    @BeforeAll
    static void startGreenMail() {
        greenMail = new GreenMail(ServerSetupTest.SMTP);
        greenMail.start();
    }

    @AfterAll
    static void stopGreenMail() {
        if (greenMail != null) {
            greenMail.stop();
        }
    }

    @Test
    void sendRegistrationOtpEmailShouldDeliverMessageToLocalSmtpServer() throws Exception {
        boolean sent = smtpEmailService.sendRegistrationOtpEmail(
                "customer@example.com",
                "Customer One",
                "123456",
                5
        );

        assertTrue(sent);
        assertTrue(waitForMessages(1, 5000));

        MimeMessage[] received = greenMail.getReceivedMessages();
        assertEquals(1, received.length);
        assertEquals("Pizza Store - Registration OTP", received[0].getSubject());
        assertEquals("customer@example.com", ((InternetAddress) received[0].getAllRecipients()[0]).getAddress());
        assertEquals("test-sender@example.com", ((InternetAddress) received[0].getFrom()[0]).getAddress());
    }

    private boolean waitForMessages(int expectedCount, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (greenMail.getReceivedMessages().length >= expectedCount) {
                return true;
            }
            Thread.sleep(100);
        }
        return greenMail.getReceivedMessages().length >= expectedCount;
    }

    @Configuration
    static class MailTestConfig {

        @Bean
        JavaMailSender javaMailSender(
                @Value("${spring.mail.host}") String host,
                @Value("${spring.mail.port}") int port
        ) {
            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost(host);
            sender.setPort(port);
            java.util.Properties props = sender.getJavaMailProperties();
            props.put("mail.smtp.auth", "false");
            props.put("mail.smtp.starttls.enable", "false");
            props.put("mail.smtp.starttls.required", "false");
            return sender;
        }
    }
}
