package com.pizzastore.controller;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LoginRequest", description = "Thong tin dang nhap de nhan access token va refresh token")
public class LoginRequest {
    @Schema(description = "Username dang nhap. Voi khach hang thuong la so dien thoai", example = "0912345678")
    private String username;

    @Schema(description = "Mat khau tai khoan", example = "123456")
    private String password;

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
