package com.pizzastore.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "RegisterOtpRequest",
        description = "Thong tin dau vao de yeu cau gui OTP dang ky qua email. Day la buoc 1, chua tao tai khoan."
)
public class RegisterOtpRequest {
    @Schema(description = "Ho ten khach hang", example = "Nguyen Van A")
    private String fullName;

    @Schema(description = "So dien thoai dang ky, dong thoi se duoc dung lam username dang nhap", example = "0912345678")
    private String phoneNumber;

    @Schema(description = "Dia chi cua khach hang", example = "123 Le Loi, Quan 1, TP.HCM")
    private String address;

    @Schema(description = "Email de nhan ma OTP dang ky va thong bao sau khi tao tai khoan thanh cong", example = "nguyenvana@gmail.com")
    private String email;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
