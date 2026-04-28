package com.pizzastore.controller;

import com.pizzastore.dto.BranchDetailResponse;
import com.pizzastore.entity.Account;
import com.pizzastore.entity.Branch;
import com.pizzastore.entity.Employee;
import com.pizzastore.entity.Order;
import com.pizzastore.enums.RoleName;
import com.pizzastore.repository.AccountRepository;
import com.pizzastore.repository.BranchRepository;
import com.pizzastore.repository.EmployeeRepository;
import com.pizzastore.repository.OrderRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @Autowired
    public BranchController(BranchRepository branchRepository,
                            EmployeeRepository employeeRepository,
                            OrderRepository orderRepository,
                            AccountRepository accountRepository) {
        this.branchRepository = branchRepository;
        this.employeeRepository = employeeRepository;
        this.orderRepository = orderRepository;
        this.accountRepository = accountRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Lay danh sach tat ca chi nhanh cho SUPER_ADMIN")
    public ResponseEntity<List<Branch>> getAllBranches() {
        return ResponseEntity.ok(branchRepository.findAll(Sort.by(Sort.Direction.ASC, "id")));
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
}
