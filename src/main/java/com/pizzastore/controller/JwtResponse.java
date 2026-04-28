package com.pizzastore.controller;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "JwtResponse", description = "Thong tin tra ve sau khi dang nhap hoac refresh token thanh cong")
public class JwtResponse {
    @Schema(description = "Access token dung de goi cac API duoc bao ve", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;

    @Schema(description = "Refresh token dung de xin access token moi khi access token het han", example = "2e6a790e-beb8-4b2f-96be-a80abf6c3e1f")
    private String refreshToken;

    @Schema(description = "Loai token tra ve", example = "Bearer")
    private String tokenType;

    @Schema(description = "Thoi gian song cua access token tinh bang mili giay", example = "300000")
    private long expiresIn;

    @Schema(description = "Username dang nhap", example = "0912345678")
    private String username;

    @Schema(description = "Role cua tai khoan", example = "ROLE_CUSTOMER")
    private String role;

    @Schema(description = "Ho ten nguoi dung", example = "Nguyen Van A")
    private String fullname;

    @Schema(description = "ID chi nhanh cua nhan vien noi bo. Khach hang se la null.", example = "1")
    private Long branchId;

    @Schema(description = "Ten chi nhanh cua nhan vien noi bo. Khach hang se la null.", example = "Chi nhanh Quan 1")
    private String branchName;

    public JwtResponse(String accessToken, String refreshToken, String tokenType, long expiresIn,
                       String username, String role, String fullname, Long branchId, String branchName) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.username = username;
        this.role = role;
        this.fullname = fullname;
        this.branchId = branchId;
        this.branchName = branchName;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public Long getBranchId() {
        return branchId;
    }

    public void setBranchId(Long branchId) {
        this.branchId = branchId;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }
}
