package com.pizzastore.controller;

import com.pizzastore.dto.ToppingRequest;
import com.pizzastore.entity.Product;
import com.pizzastore.entity.Topping;
import com.pizzastore.repository.ProductRepository;
import com.pizzastore.repository.ToppingRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/toppings")
@Tag(name = "9. Tùy chọn món thêm (Topping)", description = "Quản lý danh sách các topping (Viền phô mai, thêm xúc xích...)")
public class ToppingController {

    private final ToppingRepository toppingRepository;
    private final ProductRepository productRepository;

    @Autowired
    public ToppingController(ToppingRepository toppingRepository,
                             ProductRepository productRepository) {
        this.toppingRepository = toppingRepository;
        this.productRepository = productRepository;
    }

    @GetMapping
    @Operation(summary = "Xem danh sách Topping (Public)", description = "Hiển thị tất cả Topping kèm giá tiền để khách hàng chọn thêm khi đặt Pizza.")
    public ResponseEntity<List<Topping>> getAllToppings() {
        return ResponseEntity.ok(toppingRepository.findAll());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    @Operation(summary = "Thêm Topping", description = "Chỉ SUPER_ADMIN/MANAGER. Tạo lựa chọn topping mới và cấu hình nguyên liệu cần trừ kho.")
    public ResponseEntity<?> createTopping(@RequestBody ToppingRequest request) {
        try {
            Topping topping = new Topping();
            applyRequest(topping, request);
            return ResponseEntity.ok(toppingRepository.save(topping));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    @Operation(summary = "Sửa Topping", description = "Chỉ SUPER_ADMIN/MANAGER. Cập nhật tên, giá, nguyên liệu và định mức của topping.")
    public ResponseEntity<?> updateTopping(@PathVariable Long id, @RequestBody ToppingRequest request) {
        try {
            Topping topping = toppingRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy topping"));
            applyRequest(topping, request);
            return ResponseEntity.ok(toppingRepository.save(topping));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    @Operation(summary = "Xóa Topping", description = "Chỉ SUPER_ADMIN/MANAGER. Xóa topping khỏi danh sách lựa chọn.")
    public ResponseEntity<?> deleteTopping(@PathVariable Long id) {
        try {
            if (!toppingRepository.existsById(id)) {
                return ResponseEntity.notFound().build();
            }
            toppingRepository.deleteById(id);
            return ResponseEntity.ok("Đã xóa topping thành công");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Không thể xóa topping: " + e.getMessage());
        }
    }

    private void applyRequest(Topping topping, ToppingRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new RuntimeException("Tên topping không được để trống");
        }
        if (request.getPrice() == null || request.getPrice() < 0) {
            throw new RuntimeException("Giá topping phải lớn hơn hoặc bằng 0");
        }
        if (request.getProductId() == null) {
            throw new RuntimeException("Vui lòng chọn nguyên liệu cho topping");
        }
        if (request.getQuantityNeeded() == null || request.getQuantityNeeded() <= 0) {
            throw new RuntimeException("Định mức nguyên liệu phải lớn hơn 0");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Nguyên liệu không tồn tại"));

        topping.setName(request.getName().trim());
        topping.setPrice(request.getPrice());
        topping.setProduct(product);
        topping.setQuantityNeeded(request.getQuantityNeeded());
    }
}
