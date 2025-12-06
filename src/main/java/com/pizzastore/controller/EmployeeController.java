package com.pizzastore.controller;

import com.pizzastore.dto.EmployeeResponse; // Đảm bảo đã import DTO này
import com.pizzastore.enums.RoleName;
import com.pizzastore.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    @Autowired
    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    // --- 1. TẠO NHÂN VIÊN (POST) ---
    @PostMapping
    @PreAuthorize("hasRole('MANAGER')") // Chỉ Manager được tạo
    public ResponseEntity<?> createNewEmployee(@RequestBody CreateEmployeeRequest request) {
        try {
            String result = employeeService.createEmployee(
                    request.getFullName(),
                    request.getPhoneNumber(),
                    request.getPosition(),
                    request.getRole()
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi: " + e.getMessage());
        }
    }

    // --- 2. LẤY DANH SÁCH (GET) - BẠN ĐANG THIẾU CÁI NÀY ---
    @GetMapping
    @PreAuthorize("hasRole('MANAGER')") // Chỉ Manager được xem
    public ResponseEntity<List<EmployeeResponse>> getAllEmployees() {
        return ResponseEntity.ok(employeeService.getAllEmployees());
    }

    // --- 3. XÓA NHÂN VIÊN (DELETE) - BẠN ĐANG THIẾU CÁI NÀY ---
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')") // Chỉ Manager được xóa
    public ResponseEntity<?> deleteEmployee(@PathVariable Long id) {
        try {
            employeeService.deleteEmployee(id);
            return ResponseEntity.ok("Đã xóa nhân viên thành công!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    // --- 4. XEM CHI TIẾT 1 NHÂN VIÊN ---
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')") // Chỉ Manager được xem
    public ResponseEntity<?> getEmployeeById(@PathVariable Long id) {
        try {
            EmployeeResponse employee = employeeService.getEmployeeById(id);
            return ResponseEntity.ok(employee);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}

// DTO Request (Giữ nguyên ở cuối file)
class CreateEmployeeRequest {
    private String fullName;
    private String phoneNumber;
    private String position;
    private RoleName role;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public RoleName getRole() { return role; }
    public void setRole(RoleName role) { this.role = role; }
}