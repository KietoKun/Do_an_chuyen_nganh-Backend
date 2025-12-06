package com.pizzastore.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class ImageService {

    private final Cloudinary cloudinary;

    public ImageService() {
        // Thay 3 thông số của bạn vào đây
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", "dzt7upu2d",
                "api_key", "823461515982935",
                "api_secret", "nE5MHSI-uiNtgtw4IjWzYz7DxqM"
        ));
    }

    public String uploadImage(MultipartFile file) {
        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
            return uploadResult.get("secure_url").toString();
        } catch (IOException e) {
            throw new RuntimeException("Lỗi upload ảnh: " + e.getMessage());
        }
    }
}