package com.pizzastore.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class ForgotPasswordRequest {
    @Schema(description = "Username dang nhap, hien tai thuong la so dien thoai", example = "0901234567")
    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
