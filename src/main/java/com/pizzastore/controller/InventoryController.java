package com.pizzastore.controller;

import com.pizzastore.dto.StockImportRequest;
import com.pizzastore.entity.Branch;
import com.pizzastore.entity.Inventory;
import com.pizzastore.entity.InventoryBatch;
import com.pizzastore.entity.Product;
import com.pizzastore.repository.BranchRepository;
import com.pizzastore.repository.InventoryBatchRepository;
import com.pizzastore.repository.InventoryRepository;
import com.pizzastore.repository.ProductRepository;
import com.pizzastore.service.BranchAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/inventory")
@Tag(name = "3. Quan ly Kho da chi nhanh (Inventory)", description = "Quan ly danh muc nguyen lieu, ton kho tong va tung lo nhap theo chi nhanh.")
public class InventoryController {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryBatchRepository inventoryBatchRepository;
    private final BranchRepository branchRepository;
    private final BranchAccessService branchAccessService;

    @Autowired
    public InventoryController(ProductRepository productRepository,
                               InventoryRepository inventoryRepository,
                               InventoryBatchRepository inventoryBatchRepository,
                               BranchRepository branchRepository,
                               BranchAccessService branchAccessService) {
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.inventoryBatchRepository = inventoryBatchRepository;
        this.branchRepository = branchRepository;
        this.branchAccessService = branchAccessService;
    }

    @GetMapping("/products")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER', 'CHEF')")
    @Operation(summary = "Xem danh muc nguyen lieu", description = "Danh sach nguyen lieu chung cua toan he thong, chua bao gom ton kho.")
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productRepository.findAll());
    }

    @PostMapping("/products")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CHEF', 'MANAGER')")
    @Operation(summary = "Tao nguyen lieu moi", description = "Tao moi mot loai nguyen lieu trong catalog.")
    public ResponseEntity<?> addProduct(@RequestBody Product product) {
        if (productRepository.existsByName(product.getName())) {
            return ResponseEntity.badRequest().body("Nguyên liệu này đã tồn tại trong hệ thống");
        }
        return ResponseEntity.ok(productRepository.save(product));
    }

    @DeleteMapping("/products/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    @Operation(summary = "Xoa nguyen lieu khoi danh muc", description = "Khong nen xoa neu nguyen lieu dang nam trong cong thuc hoac con ton kho.")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        if (!productRepository.existsById(id)) {
            return ResponseEntity.badRequest().body("Nguyên liệu không tồn tại");
        }
        productRepository.deleteById(id);
        return ResponseEntity.ok("Da xoa nguyen lieu khoi danh muc");
    }

    @GetMapping("/stock/{branchId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER', 'CHEF')")
    @Operation(summary = "Xem ton kho tong cua chi nhanh", description = "Tra ve tong so luong ton cua moi nguyen lieu tai mot chi nhanh.")
    public ResponseEntity<?> getStockByBranch(@PathVariable Long branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh"));

        branchAccessService.assertCanAccessBranch(currentUsername(), branch);
        return ResponseEntity.ok(inventoryRepository.findByBranch(branch));
    }

    @GetMapping("/stock/{branchId}/batches")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER', 'CHEF')")
    @Operation(
            summary = "Xem cac lo nhap kho cua chi nhanh",
            description = """
                    Tra ve tung lo nguyen lieu cua mot chi nhanh.

                    FE dung API nay de hien thi chi tiet lo nhap:
                    - quantityImported: so luong ban dau cua lo.
                    - quantityRemaining: so luong con lai cua lo.
                    - importedAt: ngay/gio nhap kho.
                    - expiredAt: ngay het han. Neu null thi xem nhu khong co han.

                    Khi dat hang, backend se tru kho theo FEFO: lo nao co expiredAt gan nhat se bi tru truoc.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Danh sach lo nhap kho cua chi nhanh",
                    content = @Content(examples = @ExampleObject(value = """
                            [
                              {
                                "id": 1,
                                "quantityImported": 10.5,
                                "quantityRemaining": 7.0,
                                "importedAt": "2026-04-22T00:00:00",
                                "expiredAt": "2026-05-22"
                              }
                            ]
                            """))
            ),
            @ApiResponse(responseCode = "403", description = "Khong co quyen xem kho")
    })
    public ResponseEntity<?> getStockBatchesByBranch(
            @Parameter(description = "ID chi nhanh can xem lo nhap", example = "1")
            @PathVariable Long branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh"));

        branchAccessService.assertCanAccessBranch(currentUsername(), branch);
        return ResponseEntity.ok(inventoryBatchRepository.findByBranchOrderByProduct_NameAscExpiredAtAscImportedAtAsc(branch));
    }

    @PostMapping("/stock/import")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'CHEF', 'MANAGER')")
    @Operation(
            summary = "Nhap kho theo lo",
            description = """
                    Nhap kho se thuc hien 2 viec:
                    - Cong vao Inventory.quantityAvailable de xem tong ton nhanh.
                    - Tao mot InventoryBatch moi de quan ly ngay nhap, ngay het han va so luong con lai cua lo.

                    FE can gui:
                    - branchId: chi nhanh nhan hang.
                    - productId: nguyen lieu.
                    - quantityAdded: so luong nhap, bat buoc > 0.
                    - importedDate: yyyy-MM-dd, co the bo trong de lay ngay hien tai.
                    - expiredAt: yyyy-MM-dd, co the bo trong neu nguyen lieu khong co han.

                    Sau nay khi order duoc tao, backend tru batch theo FEFO: First Expired, First Out.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Tao lo nhap thanh cong",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "id": 1,
                              "quantityImported": 10.5,
                              "quantityRemaining": 10.5,
                              "importedAt": "2026-04-22T00:00:00",
                              "expiredAt": "2026-05-22"
                            }
                            """))
            ),
            @ApiResponse(responseCode = "400", description = "So luong khong hop le hoac ngay het han nho hon ngay hien tai"),
            @ApiResponse(responseCode = "403", description = "Khong co quyen nhap kho")
    })
    public ResponseEntity<?> importStock(@RequestBody StockImportRequest request) {
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh"));

        branchAccessService.assertCanAccessBranch(currentUsername(), branch);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nguyên liệu"));

        if (request.getQuantityAdded() == null || request.getQuantityAdded() <= 0) {
            return ResponseEntity.badRequest().body("Số lượng nhập phải lớn hơn 0");
        }
        if (request.getExpiredAt() != null && request.getExpiredAt().isBefore(LocalDate.now())) {
            return ResponseEntity.badRequest().body("Ngày hết hạn không được nhỏ hơn ngày hiện tại");
        }

        Optional<Inventory> optInventory = inventoryRepository.findByBranchAndProduct(branch, product);
        Inventory inventory;
        if (optInventory.isPresent()) {
            inventory = optInventory.get();
            inventory.setQuantityAvailable(inventory.getQuantityAvailable() + request.getQuantityAdded());
        } else {
            inventory = new Inventory(branch, product, request.getQuantityAdded());
        }

        LocalDate importDate = request.getImportedDate() == null ? LocalDate.now() : request.getImportedDate();
        InventoryBatch batch = new InventoryBatch(
                branch,
                product,
                request.getQuantityAdded(),
                importDate.atStartOfDay(),
                request.getExpiredAt()
        );

        inventoryRepository.save(inventory);
        return ResponseEntity.ok(inventoryBatchRepository.save(batch));
    }
    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
