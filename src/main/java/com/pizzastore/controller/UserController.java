package com.pizzastore.controller;

import com.pizzastore.dto.UpdateProfileRequest;
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

    // API: Cập nhật hồ sơ (Dùng chung cho cả Khách và Nhân viên)
    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()") // Phải đăng nhập mới được sửa
    public ResponseEntity<?> updateProfile(@RequestBody UpdateProfileRequest request) {
        try {
            // Lấy username của người đang đăng nhập từ Token
            String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();

            userService.updateProfile(currentUsername, request);

            return ResponseEntity.ok("Cập nhật hồ sơ thành công!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }
}