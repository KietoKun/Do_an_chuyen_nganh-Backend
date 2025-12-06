package com.pizzastore.controller;

import com.pizzastore.dto.MenuResponse; // Import DTO mới
import com.pizzastore.entity.Dish;
import com.pizzastore.service.DishService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dishes")
public class DishController {

    private final DishService dishService;

    @Autowired
    public DishController(DishService dishService) {
        this.dishService = dishService;
    }

    // --- 1. XEM MENU (PUBLIC) ---
    // Sửa kiểu trả về: List<Dish> -> List<MenuResponse>
    // Sửa tên hàm gọi: getActiveDishes() -> getMenu()
    @GetMapping
    public ResponseEntity<List<MenuResponse>> getMenu() {
        return ResponseEntity.ok(dishService.getMenu());
    }

    // --- 2. XEM CHI TIẾT 1 MÓN (PUBLIC) ---
    @GetMapping("/{id}")
    public ResponseEntity<?> getDishById(@PathVariable Long id) {
        try {
            Dish dish = dishService.getDishById(id);
            return ResponseEntity.ok(dish);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // --- 3. THÊM MÓN (MANAGER) ---
    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'CHEF')")
    public ResponseEntity<?> addDish(@RequestBody Dish dish) {
        return ResponseEntity.ok(dishService.addDish(dish));
    }

    // --- 4. XÓA MÓN (MANAGER) ---
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> deleteDish(@PathVariable Long id) {
        dishService.deleteDish(id);
        return ResponseEntity.ok("Đã xóa món ăn thành công!");
    }

    // --- CÁC HÀM CŨ ĐÃ BỊ LOẠI BỎ (updateDish, getDishRecipe) ---
    // Vì cấu trúc Dish thay đổi lớn (Price/Recipe sang Variant),
    // các hàm update cũ không còn chạy được nữa.
    // Tạm thời chúng ta chỉ hỗ trợ Thêm (Add) và Xem (Get) trước.
    // Nếu muốn Update, cần viết lại logic phức tạp hơn (Update Variant).
}