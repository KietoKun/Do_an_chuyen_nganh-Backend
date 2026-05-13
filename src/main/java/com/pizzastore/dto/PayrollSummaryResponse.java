package com.pizzastore.dto;

import java.time.LocalDate;

public class PayrollSummaryResponse {
    private Long employeeId;
    private String employeeName;
    private Long branchId;
    private String branchName;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Double salaryPerHour;
    private Long totalWorkMinutes;
    private Double totalWorkHours;
    private Double grossSalary;
    private Double bonus;
    private Double deduction;
    private Double netSalary;

    public PayrollSummaryResponse(Long employeeId, String employeeName, Long branchId, String branchName,
                                  LocalDate periodStart, LocalDate periodEnd, Double salaryPerHour,
                                  Long totalWorkMinutes, Double totalWorkHours, Double grossSalary,
                                  Double bonus, Double deduction, Double netSalary) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.branchId = branchId;
        this.branchName = branchName;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.salaryPerHour = salaryPerHour;
        this.totalWorkMinutes = totalWorkMinutes;
        this.totalWorkHours = totalWorkHours;
        this.grossSalary = grossSalary;
        this.bonus = bonus;
        this.deduction = deduction;
        this.netSalary = netSalary;
    }

    public Long getEmployeeId() { return employeeId; }
    public String getEmployeeName() { return employeeName; }
    public Long getBranchId() { return branchId; }
    public String getBranchName() { return branchName; }
    public LocalDate getPeriodStart() { return periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public Double getSalaryPerHour() { return salaryPerHour; }
    public Long getTotalWorkMinutes() { return totalWorkMinutes; }
    public Double getTotalWorkHours() { return totalWorkHours; }
    public Double getGrossSalary() { return grossSalary; }
    public Double getBonus() { return bonus; }
    public Double getDeduction() { return deduction; }
    public Double getNetSalary() { return netSalary; }
}
