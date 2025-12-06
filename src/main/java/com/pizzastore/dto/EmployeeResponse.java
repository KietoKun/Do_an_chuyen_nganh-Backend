package com.pizzastore.dto;

public class EmployeeResponse {
    private Long id;
    private String fullName;
    private String phoneNumber;
    private String position;
    private String username;
    private String role;

    public EmployeeResponse(Long id, String fullName, String phoneNumber, String position, String username, String role) {
        this.id = id;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.position = position;
        this.username = username;
        this.role = role;
    }

    // Getter & Setter
    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getPosition() { return position; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
}