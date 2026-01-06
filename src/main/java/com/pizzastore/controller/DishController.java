package com.pizzastore.controller;

import com.pizzastore.dto.MenuResponse;
import com.pizzastore.entity.Dish;
import com.pizzastore.entity.Recipe;
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

    @GetMapping("/variants/{variantId}/recipe")
    @PreAuthorize("hasAnyRole('CHEF', 'MANAGER')") // Chỉ nội bộ được xem
    public ResponseEntity<?> getRecipeByVariant(@PathVariable Long variantId) {
        try {
            List<Recipe> recipes = dishService.getRecipeByVariantId(variantId);
            return ResponseEntity.ok(recipes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}