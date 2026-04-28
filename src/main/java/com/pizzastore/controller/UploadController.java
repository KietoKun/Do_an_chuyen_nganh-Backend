package com.pizzastore.controller;

import com.pizzastore.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@Tag(name = "10. Quản lý File & Ảnh (Upload)", description = "API hỗ trợ upload hình ảnh món ăn, avatar lên Cloudinary hoặc Server lưu trữ")
public class UploadController {

    private final ImageService imageService;

    @Autowired
    public UploadController(ImageService imageService) {
        this.imageService = imageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER', 'CHEF')")
    @Operation(summary = "Upload hình ảnh", description = "Chỉ Manager hoặc Chef được dùng. Nhận vào 1 file ảnh (định dạng multipart/form-data), đẩy lên Cloud và trả về URL ảnh trực tiếp để lưu vào Database.")
    public ResponseEntity<?> uploadImage(@RequestPart("file") MultipartFile file) {
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
