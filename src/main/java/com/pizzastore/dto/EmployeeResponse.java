package com.pizzastore.dto;

import java.time.LocalDate;

public class EmployeeResponse {
    private Long id;
    private String fullName;
    private String phoneNumber;
    private String address;
    private LocalDate dateOfBirth;
    private String email;
    private String position;
    private String username;
    private String role;
    private Long branchId;
    private String branchName;

    public EmployeeResponse(Long id, String fullName, String phoneNumber, String email, String position, String username, String role) {
        this.id = id;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.position = position;
        this.username = username;
        this.role = role;
    }

    public EmployeeResponse(Long id, String fullName, String phoneNumber, String address, LocalDate dateOfBirth,
                            String email, String position, String username, String role) {
        this(id, fullName, phoneNumber, email, position, username, role);
        this.address = address;
        this.dateOfBirth = dateOfBirth;
    }

    public EmployeeResponse(Long id, String fullName, String phoneNumber, String address, LocalDate dateOfBirth,
                            String email, String position, String username, String role, Long branchId, String branchName) {
        this(id, fullName, phoneNumber, address, dateOfBirth, email, position, username, role);
        this.branchId = branchId;
        this.branchName = branchName;
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getAddress() { return address; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public String getEmail() { return email; }
    public String getPosition() { return position; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public Long getBranchId() { return branchId; }
    public String getBranchName() { return branchName; }
}
