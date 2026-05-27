package com.pizzastore.controller;

import com.pizzastore.dto.BranchDetailResponse;
import com.pizzastore.dto.BranchRequest;
import com.pizzastore.entity.Account;
import com.pizzastore.entity.Branch;
import com.pizzastore.entity.Employee;
import com.pizzastore.entity.Inventory;
import com.pizzastore.entity.Order;
import com.pizzastore.entity.Product;
import com.pizzastore.enums.RoleName;
import com.pizzastore.repository.AccountRepository;
import com.pizzastore.repository.BranchRepository;
import com.pizzastore.repository.EmployeeRepository;
import com.pizzastore.repository.InventoryRepository;
import com.pizzastore.repository.OrderRepository;
import com.pizzastore.repository.ProductRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/branches")
@Tag(name = "8. Quan ly chi nhanh (Branch)")
public class BranchController {

    private final BranchRepository branchRepository;
    private final EmployeeRepository employeeRepository;
    private final OrderRepository orderRepository;
    private final AccountRepository accountRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    @Autowired
    public BranchController(BranchRepository branchRepository,
                            EmployeeRepository employeeRepository,
                            OrderRepository orderRepository,
                            AccountRepository accountRepository,
                            ProductRepository productRepository,
                            InventoryRepository inventoryRepository) {
        this.branchRepository = branchRepository;
        this.employeeRepository = employeeRepository;
        this.orderRepository = orderRepository;
        this.accountRepository = accountRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Lay danh sach tat ca chi nhanh cho SUPER_ADMIN")
    public ResponseEntity<List<Branch>> getAllBranches() {
        return ResponseEntity.ok(branchRepository.findAll(Sort.by(Sort.Direction.ASC, "id")));
    }

    @GetMapping("/active")
    @Operation(summary = "Lay danh sach chi nhanh dang hoat dong cho khach chon don TAKEAWAY")
    public ResponseEntity<List<Branch>> getActiveBranches() {
        return ResponseEntity.ok(branchRepository.findActiveBranches());
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    @Operation(summary = "Tao chi nhanh moi va khoi tao ton kho 0 cho cac nguyen lieu hien co")
    public ResponseEntity<?> createBranch(@RequestBody BranchRequest request) {
        try {
            validateBranchRequest(request);
            String name = request.getName().trim();
            if (branchRepository.existsByNameIgnoreCase(name)) {
                return ResponseEntity.badRequest().body("Tên chi nhánh đã tồn tại");
            }

            Branch branch = new Branch();
            applyBranchRequest(branch, request);
            Branch savedBranch = branchRepository.save(branch);
            initializeInventoryForBranch(savedBranch);

            return ResponseEntity.ok(savedBranch);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    @Operation(summary = "Cap nhat thong tin chi nhanh")
    public ResponseEntity<?> updateBranch(@PathVariable Long id, @RequestBody BranchRequest request) {
        try {
            validateBranchRequest(request);
            Branch branch = branchRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh ID: " + id));

            String requestedName = request.getName().trim();
            boolean duplicateName = branchRepository.findAll().stream()
                    .anyMatch(existing -> !existing.getId().equals(id)
                            && existing.getName() != null
                            && existing.getName().equalsIgnoreCase(requestedName));
            if (duplicateName) {
                return ResponseEntity.badRequest().body("Tên chi nhánh đã tồn tại");
            }

            applyBranchRequest(branch, request);
            return ResponseEntity.ok(branchRepository.save(branch));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    @Operation(summary = "Bat/tat trang thai hoat dong cua chi nhanh")
    public ResponseEntity<?> updateBranchActive(@PathVariable Long id, @RequestParam boolean active) {
        try {
            Branch branch = branchRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh ID: " + id));
            branch.setActive(active);
            return ResponseEntity.ok(branchRepository.save(branch));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    @Operation(summary = "Xoa mem chi nhanh bang cach khoa trang thai hoat dong")
    public ResponseEntity<?> deleteBranch(@PathVariable Long id) {
        try {
            Branch branch = branchRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh ID: " + id));
            branch.setActive(false);
            branchRepository.save(branch);
            return ResponseEntity.ok("Đã khóa chi nhánh thành công");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    @Operation(summary = "Lay thong tin chi tiet mot chi nhanh")
    public ResponseEntity<?> getBranchDetail(@PathVariable Long id) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            Account currentAccount = accountRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

            if (currentAccount.getRole() == RoleName.MANAGER) {
                Employee manager = employeeRepository.findByAccount_Username(username)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ quản lý"));
                if (manager.getBranch() == null || !manager.getBranch().getId().equals(id)) {
                    return ResponseEntity.status(403).body("Manager chỉ được xem chi nhánh của mình");
                }
            }

            Branch branch = branchRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh ID: " + id));

            List<BranchDetailResponse.EmployeeSummary> employees = employeeRepository.findByBranch_Id(id).stream()
                    .map(emp -> new BranchDetailResponse.EmployeeSummary(
                            emp.getId(),
                            emp.getFullName(),
                            emp.getPhoneNumber(),
                            emp.getPosition(),
                            emp.getAccount().getRole().name()
                    ))
                    .collect(Collectors.toList());

            List<BranchDetailResponse.OrderSummary> orders = orderRepository.findByBranch_IdOrderByOrderTimeDesc(id).stream()
                    .map(this::toOrderSummary)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(new BranchDetailResponse(
                    branch.getId(),
                    branch.getName(),
                    branch.getAddress(),
                    branch.getLatitude(),
                    branch.getLongitude(),
                    branch.getMaxServiceRadiusKm(),
                    branch.getMaxPendingCookOrders(),
                    branch.isActive(),
                    employees,
                    orders
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    private BranchDetailResponse.OrderSummary toOrderSummary(Order order) {
        return new BranchDetailResponse.OrderSummary(
                order.getId(),
                order.getOrderTime(),
                order.getAcceptedAt(),
                order.getCookingStartedAt(),
                order.getCompletedAt(),
                order.getStatus(),
                order.getDeliveryMethod(),
                order.getDeliveryAddress(),
                order.getFinalTotalPrice(),
                order.getCustomer() != null ? order.getCustomer().getFullName() : null,
                order.getHandledBy() != null ? order.getHandledBy().getFullName() : null,
                order.getCookedBy() != null ? order.getCookedBy().getFullName() : null
        );
    }

    private void validateBranchRequest(BranchRequest request) {
        if (request == null) {
            throw new RuntimeException("Dữ liệu chi nhánh không được để trống");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new RuntimeException("Tên chi nhánh không được để trống");
        }
        if (request.getAddress() == null || request.getAddress().trim().isEmpty()) {
            throw new RuntimeException("Địa chỉ chi nhánh không được để trống");
        }
        if (request.getLatitude() == null || request.getLongitude() == null) {
            throw new RuntimeException("Tọa độ chi nhánh không được để trống");
        }
        if (request.getLatitude() < -90 || request.getLatitude() > 90) {
            throw new RuntimeException("Latitude phải nằm trong khoảng -90 đến 90");
        }
        if (request.getLongitude() < -180 || request.getLongitude() > 180) {
            throw new RuntimeException("Longitude phải nằm trong khoảng -180 đến 180");
        }
        if (request.getMaxServiceRadiusKm() != null && request.getMaxServiceRadiusKm() <= 0) {
            throw new RuntimeException("Bán kính phục vụ phải lớn hơn 0");
        }
        if (request.getMaxPendingCookOrders() != null && request.getMaxPendingCookOrders() <= 0) {
            throw new RuntimeException("Giới hạn đơn bếp phải lớn hơn 0");
        }
    }

    private void applyBranchRequest(Branch branch, BranchRequest request) {
        branch.setName(request.getName().trim());
        branch.setAddress(request.getAddress().trim());
        branch.setLatitude(request.getLatitude());
        branch.setLongitude(request.getLongitude());
        branch.setMaxServiceRadiusKm(request.getMaxServiceRadiusKm() == null ? 7.0 : request.getMaxServiceRadiusKm());
        branch.setMaxPendingCookOrders(request.getMaxPendingCookOrders() == null ? 10 : request.getMaxPendingCookOrders());
        branch.setActive(request.getActive() == null || request.getActive());
    }

    private void initializeInventoryForBranch(Branch branch) {
        List<Product> products = productRepository.findAll();
        for (Product product : products) {
            boolean exists = inventoryRepository.findByBranchAndProduct(branch, product).isPresent();
            if (!exists) {
                inventoryRepository.save(new Inventory(branch, product, 0.0));
            }
        }
    }
}
