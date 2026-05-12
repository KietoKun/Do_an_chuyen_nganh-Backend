package com.pizzastore.dto;

public class PasswordResetTokenResponse {
    private String resetToken;

    public PasswordResetTokenResponse(String resetToken) {
        this.resetToken = resetToken;
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }
}
