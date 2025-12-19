package com.pizzastore.dto;

public class RegisterRequest {
    // private String username; <--- XÓA DÒNG NÀY
    private String password;
    private String fullName;
    private String phoneNumber;
    private String address;
    private String email;

    // --- Getters & Setters ---
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
