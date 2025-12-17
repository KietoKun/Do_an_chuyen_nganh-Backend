package com.pizzastore.controller;

import com.pizzastore.dto.UpdateProfileRequest;
import com.pizzastore.dto.UserProfileResponse; // Import DTO mới
import com.pizzastore.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    // --- THÊM MỚI: API LẤY THÔNG TIN ---
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()") // Đăng nhập rồi là xem được (kể cả Staff)
    public ResponseEntity<?> getProfile() {
        try {
            String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();

            // Gọi Service lấy dữ liệu
            UserProfileResponse profile = userService.getUserProfile(currentUsername);

            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    // API: Cập nhật hồ sơ (CŨ - GIỮ NGUYÊN)
    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
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