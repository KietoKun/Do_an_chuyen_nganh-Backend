package com.pizzastore.service;

import com.pizzastore.dto.MenuResponse;
import com.pizzastore.entity.Dish;
import com.pizzastore.entity.DishVariant;
import com.pizzastore.entity.Product;
import com.pizzastore.entity.Recipe;
import com.pizzastore.repository.DishRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DishService {

    private final DishRepository dishRepository;
    private final com.pizzastore.repository.DishVariantRepository dishVariantRepository;
    @Autowired
    public DishService(DishRepository dishRepository, com.pizzastore.repository.DishVariantRepository dishVariantRepository) {
        this.dishRepository = dishRepository;
        this.dishVariantRepository = dishVariantRepository;
    }

    // 1. LẤY MENU
    public List<MenuResponse> getMenu() {
        List<Dish> dishes = dishRepository.findByIsAvailableTrue();
        return dishes.stream().map(dish -> {
            List<MenuResponse.VariantDto> variantDtos = dish.getVariants().stream().map(v ->
                    new MenuResponse.VariantDto(
                            v.getId(), v.getSize(), v.getPrice(), calculateMaxQuantity(v)
                    )
            ).collect(Collectors.toList());

            return new MenuResponse(
                    dish.getId(), dish.getName(), dish.getDescription(),
                    dish.getImageUrl(), (dish.getCategory() != null) ? dish.getCategory().getName() : "Uncategorized",
                    variantDtos
            );
        }).collect(Collectors.toList());
    }

    // 2. TÍNH SỐ LƯỢNG TỐI ĐA
    public int calculateMaxQuantity(DishVariant variant) {
        if (variant.getRecipes().isEmpty()) return 999;
        int maxCanCook = Integer.MAX_VALUE;
        for (Recipe recipe : variant.getRecipes()) {
            Product product = recipe.getProduct();
            if (recipe.getQuantityNeeded() == 0) continue;
            int possible = (int) (product.getStockQuantity() / recipe.getQuantityNeeded());
            if (possible < maxCanCook) maxCanCook = possible;
        }
        return maxCanCook;
    }
    // --- 3. THÊM HÀM LẤY CÔNG THỨC THEO VARIANT ID ---
    public List<Recipe> getRecipeByVariantId(Long variantId) {
        DishVariant variant = dishVariantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Size/Biến thể không tồn tại!"));

        // Trả về danh sách công thức của đúng cái Size đó
        return variant.getRecipes();
    }
    // 3. CÁC HÀM KHÁC
    public Dish getDishById(Long id) {
        return dishRepository.findById(id).orElseThrow(() -> new RuntimeException("Món không tồn tại"));
    }

    public Dish addDish(Dish dish) {
        return dishRepository.save(dish);
    }

    public void deleteDish(Long id) {
        dishRepository.deleteById(id);
    }


    public void refreshAllDishesAvailability() {
        List<Dish> allDishes = dishRepository.findAll();
        for (Dish dish : allDishes) {
            refreshDishAvailability(dish);
        }
    }
    // 4. CẬP NHẬT TRẠNG THÁI (Auto-update)
    @Transactional
    public void refreshDishAvailability(Dish dish) {
        boolean isAtLeastOneVariantAvailable = false;
        for (DishVariant variant : dish.getVariants()) {
            if (calculateMaxQuantity(variant) > 0) {
                isAtLeastOneVariantAvailable = true;
                break;
            }
        }
        if (dish.isAvailable() != isAtLeastOneVariantAvailable) {
            dish.setAvailable(isAtLeastOneVariantAvailable);
            dishRepository.save(dish);
            System.out.println(">>> Auto Update Dish: " + dish.getName() + " -> " + isAtLeastOneVariantAvailable);
        }
    }
}