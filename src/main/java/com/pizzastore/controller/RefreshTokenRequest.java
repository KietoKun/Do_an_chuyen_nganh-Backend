package com.pizzastore.controller;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "RefreshTokenRequest", description = "Du lieu dau vao de xin access token moi bang refresh token")
public class RefreshTokenRequest {
    @Schema(description = "Refresh token duoc cap khi dang nhap thanh cong", example = "2e6a790e-beb8-4b2f-96be-a80abf6c3e1f")
    private String refreshToken;

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
