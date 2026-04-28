package com.pizzastore.controller;

import com.pizzastore.dto.UpdateProfileRequest;
import com.pizzastore.dto.UserProfileResponse;
import com.pizzastore.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Tag(name = "11. Thông tin Cá nhân (User Profile)", description = "API quản lý hồ sơ cá nhân của người dùng đang đăng nhập (áp dụng cho cả Khách hàng và Nhân viên)")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Xem hồ sơ cá nhân", description = "Hệ thống tự động đọc JWT Token để nhận diện người dùng hiện tại và trả về thông tin hồ sơ (Tên, SĐT, Email, Địa chỉ...).")
    public ResponseEntity<?> getProfile() {
        try {
            String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();

            UserProfileResponse profile = userService.getUserProfile(currentUsername);

            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cập nhật hồ sơ cá nhân", description = "Cho phép người dùng đang đăng nhập tự sửa đổi thông tin cá nhân của mình.")
    public ResponseEntity<?> updateProfile(@RequestBody UpdateProfileRequest request) {
        try {
            String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
            userService.updateProfile(currentUsername, request);
            return ResponseEntity.ok("Cập nhật hồ sơ thành công!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }
}