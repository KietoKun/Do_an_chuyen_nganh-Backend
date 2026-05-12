package com.pizzastore.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class ResetPasswordRequest {
    @Schema(description = "Username dang nhap, hien tai thuong la so dien thoai", example = "0901234567")
    private String username;

    @Schema(description = "Reset token tra ve sau khi xac thuc OTP thanh cong", example = "Yxj4OEU0vO1ReFnd0xJ5DoL4M_4CHwvf")
    private String resetToken;

    @Schema(description = "Mat khau moi", example = "NewPassword@123")
    private String newPassword;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
