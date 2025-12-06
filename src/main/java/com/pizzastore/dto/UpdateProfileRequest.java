package com.pizzastore.dto;

public class UpdateProfileRequest {
    // private String username; <--- XÓA DÒNG NÀY
    private String fullName;
    private String phoneNumber; // Có thể giữ lại để update SĐT liên lạc (nhưng không đổi username/account)
    private String address;
    private String email;

    public UpdateProfileRequest() {}

    // --- XÓA GETTER/SETTER CỦA USERNAME LUÔN ---

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}