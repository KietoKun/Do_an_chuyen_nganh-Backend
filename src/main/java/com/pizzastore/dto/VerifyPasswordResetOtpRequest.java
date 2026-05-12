package com.pizzastore.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class VerifyPasswordResetOtpRequest {
    @Schema(description = "Username dang nhap, hien tai thuong la so dien thoai", example = "0901234567")
    private String username;

    @Schema(description = "Ma OTP nhan duoc qua email", example = "123456")
    private String otpCode;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }
}
