package com.pizzastore.service;

import com.pizzastore.dto.DishDetailResponse;
import com.pizzastore.dto.MenuResponse;
import com.pizzastore.entity.*;
import com.pizzastore.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DishService {

    private final DishRepository dishRepository;
    private final DishVariantRepository dishVariantRepository;
    private final InventoryRepository inventoryRepository; // ĐÃ THÊM
    private final BranchRepository branchRepository;
    private final CategoryRepository categoryRepository; // ĐÃ THÊM
    private final ProductRepository productRepository;
    @Autowired
    public DishService(DishRepository dishRepository,
                       DishVariantRepository dishVariantRepository,
                       InventoryRepository inventoryRepository,
                       BranchRepository branchRepository,
                       CategoryRepository categoryRepository,
                       ProductRepository productRepository) {
        this.dishRepository = dishRepository;
        this.dishVariantRepository = dishVariantRepository;
        this.inventoryRepository = inventoryRepository;
        this.branchRepository = branchRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    // NÂNG CẤP: Truyền thêm branchId vào để tính toán số lượng theo chi nhánh
    public List<MenuResponse> getMenu() {
        // Chỉ lấy các món đang được phép kinh doanh (Manager không tắt)
        List<Dish> dishes = dishRepository.findByIsAvailableTrue();

        List<MenuResponse> menu = new ArrayList<>();

        for (Dish dish : dishes) {
            List<MenuResponse.VariantDto> variantDtos = dish.getVariants().stream().map(v -> {
                int maxQty = calculateMaxQuantityAcrossBranches(v);
                return new MenuResponse.VariantDto(v.getId(), v.getSize(), v.getPrice(), maxQty);
            }).collect(Collectors.toList());

            menu.add(new MenuResponse(
                    dish.getId(), dish.getName(), dish.getDescription(),
                    dish.getImageUrl(), (dish.getCategory() != null) ? dish.getCategory().getName() : "Uncategorized",
                    variantDtos
            ));
        }
        return menu;
    }

    // NÂNG CẤP: Tính số lượng dựa trên Bảng Inventory của Chi nhánh cụ thể
    public int calculateMaxQuantity(DishVariant variant, Branch branch) {
        if (variant.getRecipes().isEmpty()) return 999;

        int maxCanCook = Integer.MAX_VALUE;
        for (Recipe recipe : variant.getRecipes()) {
            Product product = recipe.getProduct();
            if (recipe.getQuantityNeeded() == 0) continue;

            // Moi kho của chi nhánh này ra
            Inventory inventory = inventoryRepository.findByBranchAndProduct(branch, product).orElse(null);

            double availableStock = (inventory != null) ? inventory.getQuantityAvailable() : 0.0;
            int possible = (int) (availableStock / recipe.getQuantityNeeded());

            if (possible < maxCanCook) {
                maxCanCook = possible;
            }
        }
        return maxCanCook == Integer.MAX_VALUE ? 0 : maxCanCook;
    }

    public int calculateMaxQuantityAcrossBranches(DishVariant variant) {
        List<Branch> branches = branchRepository.findAll().stream()
                .filter(Branch::isActive)
                .collect(Collectors.toList());

        if (branches.isEmpty()) {
            return 0;
        }

        int maxQuantity = 0;
        for (Branch branch : branches) {
            maxQuantity = Math.max(maxQuantity, calculateMaxQuantity(variant, branch));
        }
        return maxQuantity;
    }

    public List<Recipe> getRecipeByVariantId(Long variantId) {
        DishVariant variant = dishVariantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Size/Biến thể không tồn tại!"));
        return variant.getRecipes();
    }

    public Dish getDishEntityById(Long id) {
        return dishRepository.findById(id).orElseThrow(() -> new RuntimeException("Món không tồn tại"));
    }

    public DishDetailResponse getDishById(Long id) {
        Dish dish = dishRepository.findById(id).orElseThrow(() -> new RuntimeException("Món không tồn tại"));

        List<DishDetailResponse.VariantDto> variants = dish.getVariants().stream().map(variant -> {
            int maxQty = calculateMaxQuantityAcrossBranches(variant);
            return new DishDetailResponse.VariantDto(
                    variant.getId(),
                    variant.getSize(),
                    variant.getPrice(),
                    dish.getName(),
                    maxQty
            );
        }).collect(Collectors.toList());

        DishDetailResponse.CategoryDto category = dish.getCategory() != null
                ? new DishDetailResponse.CategoryDto(dish.getCategory().getId(), dish.getCategory().getName())
                : null;

        return new DishDetailResponse(
                dish.getId(),
                dish.getName(),
                dish.getDescription(),
                dish.getImageUrl(),
                category,
                variants
        );
    }

    @Transactional
    public Dish addDish(com.pizzastore.dto.DishRequest request) {
        Dish dish = new Dish();
        dish.setName(request.getName());
        dish.setDescription(request.getDescription());
        dish.setImageUrl(request.getImageUrl());
        dish.setAvailable(true); // Mặc định mở bán

        // 1. Gắn Category
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Category ID: " + request.getCategoryId()));
            dish.setCategory(category);
        }

        // 2. Duyệt qua từng Variant (Size)
        if (request.getVariants() != null) {
            for (com.pizzastore.dto.DishRequest.VariantRequest vReq : request.getVariants()) {
                DishVariant variant = new DishVariant();
                variant.setSize(vReq.getSize());
                variant.setPrice(vReq.getPrice());
                variant.setDish(dish); // Bắt buộc để map 2 chiều

                // 3. Duyệt qua từng Recipe của Variant đó
                if (vReq.getRecipes() != null) {
                    for (com.pizzastore.dto.DishRequest.RecipeRequest rReq : vReq.getRecipes()) {
                        Product product = productRepository.findById(rReq.getProductId())
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy Nguyên liệu ID: " + rReq.getProductId()));

                        Recipe recipe = new Recipe();
                        recipe.setDishVariant(variant);
                        recipe.setProduct(product);
                        recipe.setQuantityNeeded(rReq.getQuantityNeeded());

                        variant.getRecipes().add(recipe);
                    }
                }
                dish.getVariants().add(variant);
            }
        }

        // Lưu 1 phát ăn ngay: Hibernate sẽ tự lưu Dish -> DishVariant -> Recipe (Nếu bạn cài CascadeType.ALL chuẩn)
        return dishRepository.save(dish);
    }

    public void deleteDish(Long id) {
        dishRepository.deleteById(id);
    }

    // LƯU Ý: Đã xóa 2 hàm refreshAllDishesAvailability() và refreshDishAvailability()
    // Vì việc bật/tắt isAvailable bây giờ không còn phụ thuộc vào kho tự động nữa.
}
