package com.pizzastore.controller;

import com.pizzastore.service.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType; // <--- 1. Import cái này
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private final ImageService imageService;

    @Autowired
    public UploadController(ImageService imageService) {
        this.imageService = imageService;
    }

    // API Upload ảnh
    // Thêm consumes = MediaType.MULTIPART_FORM_DATA_VALUE để Swagger hiểu
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('MANAGER', 'CHEF')")
    public ResponseEntity<?> uploadImage(@RequestPart("file") MultipartFile file) { // <--- Đổi @RequestParam thành @RequestPart
        try {
            String url = imageService.uploadImage(file);

            Map<String, String> response = new HashMap<>();
            response.put("url", url);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi upload: " + e.getMessage());
        }
    }
}