package com.pizzastore.dto;

import com.pizzastore.enums.PayrollStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class PayrollRecordResponse extends PayrollSummaryResponse {
    private Long id;
    private PayrollStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime paidAt;

    public PayrollRecordResponse(Long id, Long employeeId, String employeeName, Long branchId, String branchName,
                                 LocalDate periodStart, LocalDate periodEnd, Double salaryPerHour,
                                 Long totalWorkMinutes, Double totalWorkHours, Double grossSalary,
                                 Double bonus, Double deduction, Double netSalary, PayrollStatus status,
                                 LocalDateTime createdAt, LocalDateTime confirmedAt, LocalDateTime paidAt) {
        super(employeeId, employeeName, branchId, branchName, periodStart, periodEnd, salaryPerHour,
                totalWorkMinutes, totalWorkHours, grossSalary, bonus, deduction, netSalary);
        this.id = id;
        this.status = status;
        this.createdAt = createdAt;
        this.confirmedAt = confirmedAt;
        this.paidAt = paidAt;
    }

    public Long getId() { return id; }
    public PayrollStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public LocalDateTime getPaidAt() { return paidAt; }
}
