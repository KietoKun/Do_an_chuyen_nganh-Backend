package com.pizzastore.dto;

import com.pizzastore.enums.AttendanceStatus;

import java.time.LocalDateTime;

public class AttendanceRecordResponse {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private Long branchId;
    private String branchName;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private Long workMinutes;
    private Double workHours;
    private AttendanceStatus status;
    private String note;

    public AttendanceRecordResponse(Long id, Long employeeId, String employeeName, Long branchId, String branchName,
                                    LocalDateTime checkInTime, LocalDateTime checkOutTime, Long workMinutes,
                                    Double workHours, AttendanceStatus status, String note) {
        this.id = id;
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.branchId = branchId;
        this.branchName = branchName;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
        this.workMinutes = workMinutes;
        this.workHours = workHours;
        this.status = status;
        this.note = note;
    }

    public Long getId() { return id; }
    public Long getEmployeeId() { return employeeId; }
    public String getEmployeeName() { return employeeName; }
    public Long getBranchId() { return branchId; }
    public String getBranchName() { return branchName; }
    public LocalDateTime getCheckInTime() { return checkInTime; }
    public LocalDateTime getCheckOutTime() { return checkOutTime; }
    public Long getWorkMinutes() { return workMinutes; }
    public Double getWorkHours() { return workHours; }
    public AttendanceStatus getStatus() { return status; }
    public String getNote() { return note; }
}
