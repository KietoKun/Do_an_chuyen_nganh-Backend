package com.pizzastore.service;

import com.pizzastore.dto.EmployeeResponse; // Import DTO
import com.pizzastore.entity.Account;
import com.pizzastore.entity.Employee;
import com.pizzastore.enums.RoleName;
import com.pizzastore.repository.AccountRepository;
import com.pizzastore.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// --- CÁC IMPORT BẠN ĐANG THIẾU ---
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
// ---------------------------------

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final AccountRepository accountRepository; // Thêm nếu cần dùng riêng
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public EmployeeService(EmployeeRepository employeeRepository,
                           AccountRepository accountRepository,
                           PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // --- 1. TẠO NHÂN VIÊN (Logic cũ) ---
    @Transactional
    public String createEmployee(String fullName, String phone, String position, RoleName role) {

        // 1. Kiểm tra logic an toàn
        if (role == RoleName.CUSTOMER) {
            throw new RuntimeException("Lỗi: Không thể tạo nhân viên với quyền CUSTOMER!");
        }

        // 2. KIỂM TRA SĐT TRÙNG (Quan trọng vì giờ nó là Username)
        if (accountRepository.existsByUsername(phone)) {
            throw new RuntimeException("Lỗi: Số điện thoại (Tài khoản) này đã tồn tại!");
        }

        // 3. Tạo Account (Dùng SĐT làm Username)
        String username = phone; // <--- CHỐT: Username là SĐT

        // Sinh mật khẩu ngẫu nhiên
        String rawPassword = UUID.randomUUID().toString().substring(0, 6);

        Account account = new Account();
        account.setUsername(username); // Set username = phone
        account.setPassword(passwordEncoder.encode(rawPassword));
        account.setRole(role);
        account.setFirstLogin(true); // Bắt đổi pass lần đầu

        // 4. Tạo Employee
        Employee employee = new Employee();
        employee.setFullName(fullName);
        employee.setPhoneNumber(phone); // Lưu SĐT vào thông tin cá nhân
        // employee.setAddress(...); // Nếu có thì set thêm

        employee.setPosition(position);
        employee.setAccount(account);

        employeeRepository.save(employee);

        return "Tạo thành công! Tài khoản (SĐT): " + username + " | Mật khẩu: " + rawPassword;
    }

    // --- 2. LẤY DANH SÁCH (Hàm bạn đang bị lỗi) ---
    public List<EmployeeResponse> getAllEmployees() {
        // Lấy danh sách Entity từ DB
        List<Employee> employees = employeeRepository.findAll();

        // Convert từ Entity sang DTO
        // Lưu ý: Đảm bảo class Employee có đủ các hàm get...()
        return employees.stream().map(emp -> new EmployeeResponse(
                emp.getId(),
                emp.getFullName(),
                emp.getPhoneNumber(),
                emp.getPosition(),
                emp.getAccount().getUsername(),
                emp.getAccount().getRole().name()
        )).collect(Collectors.toList());
    }

    // --- 3. XÓA NHÂN VIÊN ---
    public void deleteEmployee(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy nhân viên ID: " + id);
        }
        employeeRepository.deleteById(id);
    }

    public String checkInEmployee(Long employeeId) {
        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Nhân viên không tồn tại!"));

        // Gọi phương thức nghiệp vụ trong Entity
        emp.clockIn();

        // Lưu trạng thái mới vào DB
        employeeRepository.save(emp);

        return "Chấm công thành công cho: " + emp.getFullName() + " vào lúc " + emp.getLastCheckIn();
    }

    // --- 4. LẤY CHI TIẾT 1 NHÂN VIÊN ---
    public EmployeeResponse getEmployeeById(Long id) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên với ID: " + id));

        // Convert Entity -> DTO
        return new EmployeeResponse(
                emp.getId(),
                emp.getFullName(),
                emp.getPhoneNumber(),
                emp.getPosition(),
                emp.getAccount().getUsername(),
                emp.getAccount().getRole().name()
        );
    }
}