package com.pizzastore.controller;

import com.pizzastore.dto.MenuResponse;
import com.pizzastore.entity.Recipe;
import com.pizzastore.service.DishService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dishes")
@Tag(name = "2. Quản lý Menu & Món ăn (Dish)", description = "Các API dùng để xem Menu, thêm/xóa món ăn và định nghĩa công thức nấu (Recipe)")
public class DishController {

    private final DishService dishService;

    @Autowired
    public DishController(DishService dishService) {
        this.dishService = dishService;
    }

    @GetMapping
    @Operation(summary = "Xem Menu Món ăn", description = "Hiển thị danh sách món đang bán chung cho toàn hệ thống.")
    public ResponseEntity<List<MenuResponse>> getMenu() {
        return ResponseEntity.ok(dishService.getMenu());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết 1 Món ăn", description = "Tìm kiếm thông tin chi tiết, hình ảnh và danh sách các Size của món ăn thông qua ID.")
    public ResponseEntity<?> getDishById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(dishService.getDishById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER', 'CHEF')")
    @Operation(summary = "Tạo Món ăn mới (Kèm công thức)", description = "Chỉ Manager/Chef. Cho phép tạo Món ăn, khai báo các Size và cấu hình Nguyên liệu cho từng Size trong cùng 1 lần tạo.")
    public ResponseEntity<?> addDish(@RequestBody com.pizzastore.dto.DishRequest request) {
        return ResponseEntity.ok(dishService.addDish(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    @Operation(summary = "Xóa Món ăn", description = "Chỉ Manager. Xóa món ăn khỏi Menu của hệ thống.")
    public ResponseEntity<?> deleteDish(@PathVariable Long id) {
        dishService.deleteDish(id);
        return ResponseEntity.ok("Đã xóa món ăn thành công!");
    }

    @GetMapping("/variants/{variantId}/recipe")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CHEF', 'MANAGER')")
    @Operation(summary = "Xem Công thức nấu của 1 Size (Nội bộ)", description = "Dành cho Bếp trưởng hoặc Quản lý xem chi tiết định mức nguyên liệu cần thiết để làm ra size bánh này.")
    public ResponseEntity<?> getRecipeByVariant(@PathVariable Long variantId) {
        try {
            List<Recipe> recipes = dishService.getRecipeByVariantId(variantId);
            return ResponseEntity.ok(recipes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
