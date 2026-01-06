package com.pizzastore.controller;

import com.pizzastore.entity.Product;
import com.pizzastore.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final ProductRepository productRepository;
    private final com.pizzastore.service.DishService dishService;

    @Autowired
    public InventoryController(ProductRepository productRepository, com.pizzastore.service.DishService dishService) {
        this.productRepository = productRepository;
        this.dishService = dishService;
    }

    // --- 1. LẤY DANH SÁCH NGUYÊN LIỆU (Xem kho) ---
    @GetMapping("/products")
    @PreAuthorize("hasAnyRole('MANAGER', 'CHEF')")
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productRepository.findAll());
    }

    // --- 2. NHẬP KHO / THÊM NGUYÊN LIỆU MỚI ---
    // Chỉ Chef (hoặc Manager) mới được nhập hàng
    @PostMapping("/products")
    @PreAuthorize("hasAnyRole('CHEF', 'MANAGER')")
    public ResponseEntity<?> addOrUpdateProduct(@RequestBody Product product) {
        Product savedProduct = productRepository.save(product);
        dishService.refreshAllDishesAvailability();
        return ResponseEntity.ok(savedProduct);
    }

    // --- 3. XÓA NGUYÊN LIỆU (Nếu nhập sai) ---
    @DeleteMapping("/products/{id}")
    @PreAuthorize("hasRole('MANAGER')") // Chỉ Manager được xóa
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        if (!productRepository.existsById(id)) {
            return ResponseEntity.badRequest().body("Nguyên liệu không tồn tại!");
        }
        productRepository.deleteById(id);
        return ResponseEntity.ok("Đã xóa nguyên liệu kho!");
    }
}