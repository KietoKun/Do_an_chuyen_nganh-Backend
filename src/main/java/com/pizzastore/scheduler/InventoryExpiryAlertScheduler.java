package com.pizzastore.scheduler;

import com.pizzastore.entity.Branch;
import com.pizzastore.entity.Employee;
import com.pizzastore.entity.InventoryBatch;
import com.pizzastore.enums.RoleName;
import com.pizzastore.repository.EmployeeRepository;
import com.pizzastore.repository.InventoryBatchRepository;
import com.pizzastore.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class InventoryExpiryAlertScheduler {
    private static final Logger logger = LoggerFactory.getLogger(InventoryExpiryAlertScheduler.class);

    private final InventoryBatchRepository inventoryBatchRepository;
    private final EmployeeRepository employeeRepository;
    private final EmailService emailService;

    @Value("${app.inventory.expiry-alert-days:2}")
    private int expiryAlertDays;

    @Value("${spring.mail.username:}")
    private String fallbackEmail;

    public InventoryExpiryAlertScheduler(InventoryBatchRepository inventoryBatchRepository,
                                         EmployeeRepository employeeRepository,
                                         EmailService emailService) {
        this.inventoryBatchRepository = inventoryBatchRepository;
        this.employeeRepository = employeeRepository;
        this.emailService = emailService;
    }

    @Scheduled(cron = "0 0 7 * * ?")
    public void sendInventoryExpiryAlerts() {
        LocalDate today = LocalDate.now();
        LocalDate alertUntil = today.plusDays(expiryAlertDays);

        List<InventoryBatch> expiringBatches = inventoryBatchRepository.findExpiringBatches(today, alertUntil);
        if (expiringBatches.isEmpty()) {
            logger.info("No inventory batches expiring between {} and {}", today, alertUntil);
            return;
        }

        Map<Branch, List<InventoryBatch>> batchesByBranch = new LinkedHashMap<>();
        for (InventoryBatch batch : expiringBatches) {
            batchesByBranch.computeIfAbsent(batch.getBranch(), ignored -> new java.util.ArrayList<>()).add(batch);
        }

        for (Map.Entry<Branch, List<InventoryBatch>> entry : batchesByBranch.entrySet()) {
            Branch branch = entry.getKey();
            String recipientEmail = resolveRecipientEmail(branch);
            if (recipientEmail == null || recipientEmail.isBlank()) {
                logger.warn("Inventory expiry alert skipped for branch {} because no recipient email is configured", branch.getName());
                continue;
            }

            String subject = "Pizza Store - Canh bao nguyen lieu sap het han";
            String content = buildEmailContent(branch, entry.getValue(), today);
            boolean sent = emailService.sendInventoryExpiryAlertEmail(recipientEmail, subject, content);
            if (sent) {
                logger.info("Inventory expiry alert sent to {} for branch {}", recipientEmail, branch.getName());
            }
        }
    }

    private String resolveRecipientEmail(Branch branch) {
        List<Employee> managers = employeeRepository.findByBranch_IdAndAccount_Role(branch.getId(), RoleName.MANAGER);
        for (Employee manager : managers) {
            if (manager.getEmail() != null && !manager.getEmail().isBlank()) {
                return manager.getEmail();
            }
        }
        return fallbackEmail;
    }

    private String buildEmailContent(Branch branch, List<InventoryBatch> batches, LocalDate today) {
        StringBuilder content = new StringBuilder();
        content.append("Canh bao nguyen lieu sap het han tai chi nhanh: ")
                .append(branch.getName())
                .append("\n\n");
        content.append("Cac lo sau con ton kho va se het han trong ")
                .append(expiryAlertDays)
                .append(" ngay:\n\n");

        for (InventoryBatch batch : batches) {
            long daysLeft = ChronoUnit.DAYS.between(today, batch.getExpiredAt());
            content.append("- ")
                    .append(batch.getProduct().getName())
                    .append(" | Con lai: ")
                    .append(batch.getQuantityRemaining())
                    .append(" ")
                    .append(batch.getProduct().getUnit())
                    .append(" | Het han: ")
                    .append(batch.getExpiredAt())
                    .append(" | Con ")
                    .append(daysLeft)
                    .append(" ngay\n");
        }

        content.append("\nVui long kiem tra kho va uu tien xu ly cac lo nay.");
        return content.toString();
    }
}
