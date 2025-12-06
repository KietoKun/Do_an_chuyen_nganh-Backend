package com.pizzastore.dto;

public class CustomerResponse {
    private Long id;
    private String fullName;
    private String phoneNumber;
    private String address;
    private String email;
    private String username; // Username (chính là SĐT đăng nhập)

    public CustomerResponse(Long id, String fullName, String phoneNumber, String address, String email, String username) {
        this.id = id;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.email = email;
        this.username = username;
    }

    // Getters
    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getAddress() { return address; }
    public String getEmail() { return email; }
    public String getUsername() { return username; }
}