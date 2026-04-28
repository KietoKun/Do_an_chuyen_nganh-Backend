package com.pizzastore.controller;

import com.pizzastore.dto.CreateEmployeeRequest;
import com.pizzastore.dto.EmployeeResponse;
import com.pizzastore.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@Tag(name = "6. Quản lý Nhân sự (Employee)", description = "API quản lý thông tin nhân viên, cấp phát tài khoản và quyền hạn")
public class EmployeeController {

    private final EmployeeService employeeService;

    @Autowired
    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    @Operation(summary = "Tạo hồ sơ và Cấp tài khoản Nhân viên", description = "Chỉ Manager. Nhập thông tin nhân sự và chọn quyền (STAFF/CHEF). Hệ thống tự động tạo tài khoản đăng nhập bằng Số điện thoại.")
    public ResponseEntity<?> createNewEmployee(@RequestBody CreateEmployeeRequest request) {
        try {
            String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
            String result = employeeService.createEmployee(
                    request.getFullName(),
                    request.getPhoneNumber(),
                    request.getAddress(),
                    request.getDateOfBirth(),
                    request.getEmail(),
                    request.getPosition(),
                    request.getRole(),
                    request.getBranchId(),
                    currentUsername
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    @Operation(summary = "Lấy danh sách Nhân viên", description = "Chỉ Manager. Xem toàn bộ danh sách nhân sự đang làm việc tại cửa hàng.")
    public ResponseEntity<List<EmployeeResponse>> getAllEmployees() {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(employeeService.getAllEmployees(currentUsername));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    @Operation(summary = "Xóa / Cho thôi việc Nhân viên", description = "Chỉ Manager. Xóa hồ sơ nhân viên và vô hiệu hóa tài khoản đăng nhập của họ.")
    public ResponseEntity<?> deleteEmployee(@PathVariable Long id) {
        try {
            String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
            employeeService.deleteEmployee(id, currentUsername);
            return ResponseEntity.ok("Đã xóa nhân viên thành công!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    @Operation(summary = "Xem chi tiết 1 Nhân viên", description = "Chỉ Manager. Tra cứu thông tin hồ sơ của nhân viên dựa theo ID.")
    public ResponseEntity<?> getEmployeeById(@PathVariable Long id) {
        try {
            String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
            EmployeeResponse employee = employeeService.getEmployeeById(id, currentUsername);
            return ResponseEntity.ok(employee);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
