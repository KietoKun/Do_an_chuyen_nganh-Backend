package com.pizzastore.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "RegisterRequest",
        description = "Thong tin hoan tat dang ky. Day la buoc 2: gui kem otpCode da nhan duoc de xac thuc va tao tai khoan."
)
public class RegisterRequest {
    @Schema(description = "Mat khau cho tai khoan moi", example = "123456")
    private String password;

    @Schema(description = "Ho ten khach hang", example = "Nguyen Van A")
    private String fullName;

    @Schema(description = "So dien thoai dang ky, dong thoi la username dang nhap", example = "0912345678")
    private String phoneNumber;

    @Schema(description = "Dia chi cua khach hang", example = "123 Le Loi, Quan 1, TP.HCM")
    private String address;

    @Schema(description = "Email de nhan thong bao tao tai khoan thanh cong", example = "nguyenvana@gmail.com")
    private String email;

    @Schema(description = "Ma OTP nguoi dung nhan qua email o buoc 1", example = "123456")
    private String otpCode;

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

    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }
}
