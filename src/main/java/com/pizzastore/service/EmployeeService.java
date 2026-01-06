package com.pizzastore.service;

import com.pizzastore.dto.EmployeeResponse;
import com.pizzastore.entity.Account;
import com.pizzastore.entity.Employee;
import com.pizzastore.enums.RoleName;
import com.pizzastore.repository.AccountRepository;
import com.pizzastore.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public EmployeeService(EmployeeRepository employeeRepository,
                           AccountRepository accountRepository,
                           PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public String createEmployee(String fullName, String phone, String position, RoleName role) {

        if (role == RoleName.CUSTOMER) {
            throw new RuntimeException("Lỗi: Không thể tạo nhân viên với quyền CUSTOMER!");
        }

        if (accountRepository.existsByUsername(phone)) {
            throw new RuntimeException("Lỗi: Số điện thoại (Tài khoản) này đã tồn tại!");
        }


        String username = phone;

        String rawPassword = UUID.randomUUID().toString().substring(0, 6);

        Account account = new Account();
        account.setUsername(username);
        account.setPassword(passwordEncoder.encode(rawPassword));
        account.setRole(role);
        account.setFirstLogin(true);

        Employee employee = new Employee();
        employee.setFullName(fullName);
        employee.setPhoneNumber(phone);


        employee.setPosition(position);
        employee.setAccount(account);

        employeeRepository.save(employee);

        return "Tạo thành công! Tài khoản (SĐT): " + username + " | Mật khẩu: " + rawPassword;
    }

    public List<EmployeeResponse> getAllEmployees() {

        List<Employee> employees = employeeRepository.findAll();


        return employees.stream().map(emp -> new EmployeeResponse(
                emp.getId(),
                emp.getFullName(),
                emp.getPhoneNumber(),
                emp.getPosition(),
                emp.getAccount().getUsername(),
                emp.getAccount().getRole().name()
        )).collect(Collectors.toList());
    }
    public void deleteEmployee(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy nhân viên ID: " + id);
        }
        employeeRepository.deleteById(id);
    }

    public String checkInEmployee(Long employeeId) {
        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Nhân viên không tồn tại!"));

        emp.clockIn();

        // Lưu trạng thái mới vào DB
        employeeRepository.save(emp);

        return "Chấm công thành công cho: " + emp.getFullName() + " vào lúc " + emp.getLastCheckIn();
    }

    public EmployeeResponse getEmployeeById(Long id) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên với ID: " + id));

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