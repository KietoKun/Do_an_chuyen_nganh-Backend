package com.pizzastore.controller;

// 1. Import các Class trong dự án của bạn
import com.pizzastore.entity.Product;
import com.pizzastore.repository.ProductRepository;

// 2. Import thư viện Spring Framework (Dependency Injection & Web)
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// 3. Import thư viện Bảo mật (Để phân quyền cho Chef)
import org.springframework.security.access.prepost.PreAuthorize;

// 4. Import thư viện Java cơ bản (List)
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
    // Manager cần xem để báo cáo, Chef cần xem để nấu
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
        // Lưu xuống DB (Nếu có ID thì là update, chưa có là tạo mới)
        Product savedProduct = productRepository.save(product);
        dishService.refreshAllDishesAvailability();
        return ResponseEntity.ok(savedProduct);
    }

    // --- 3. XÓA NGUYÊN LIỆU (Nếu nhập sai) ---
    @DeleteMapping("/products/{id}")
    @PreAuthorize("hasRole('MANAGER')") // Chỉ Manager được xóa cho an toàn
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        if (!productRepository.existsById(id)) {
            return ResponseEntity.badRequest().body("Nguyên liệu không tồn tại!");
        }
        productRepository.deleteById(id);
        return ResponseEntity.ok("Đã xóa nguyên liệu kho!");
    }
}