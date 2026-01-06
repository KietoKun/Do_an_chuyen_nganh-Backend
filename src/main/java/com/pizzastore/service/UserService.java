package com.pizzastore.service;

import com.pizzastore.dto.UpdateProfileRequest;
import com.pizzastore.entity.Account;
import com.pizzastore.entity.Customer;
import com.pizzastore.entity.Employee;
import com.pizzastore.enums.RoleName;
import com.pizzastore.repository.AccountRepository;
import com.pizzastore.repository.CustomerRepository;
import com.pizzastore.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.pizzastore.dto.UserProfileResponse;

@Service
public class UserService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;

    @Autowired
    public UserService(AccountRepository accountRepository,
                       CustomerRepository customerRepository,
                       EmployeeRepository employeeRepository) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional
    public void updateProfile(String currentUsername, UpdateProfileRequest request) {
        // 1. Tìm Account đang đăng nhập
        Account account = accountRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));



        // 2. Cập nhật thông tin cá nhân (Tên, Địa chỉ, Email...)
        if (account.getRole() == RoleName.CUSTOMER) {
            updateCustomerInfo(account, request);
        } else {
            updateEmployeeInfo(account, request);
        }
    }

    public UserProfileResponse getUserProfile(String username) {
        // 1. Tìm Account gốc
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Tài khoản không tồn tại"));

        UserProfileResponse response = new UserProfileResponse();
        response.setUsername(account.getUsername());
        response.setRole(account.getRole().name()); // Lấy Role (CUSTOMER, MANAGER...)


        // Cách 1: Tìm trong bảng Customer trước
        java.util.Optional<Customer> customerOpt = customerRepository.findAll().stream()
                .filter(c -> c.getAccount().getId().equals(account.getId()))
                .findFirst();

        if (customerOpt.isPresent()) {
            Customer c = customerOpt.get();
            response.setId(c.getId());
            response.setFullName(c.getFullName());
            response.setEmail(c.getEmail());
            response.setPhoneNumber(c.getPhoneNumber());
            response.setAddress(c.getAddress()); // Khách hàng có địa chỉ
            return response;
        }

        // Cách 2: Nếu không phải Customer, tìm trong bảng Employee (Cho Admin/Staff)
        java.util.Optional<Employee> employeeOpt = employeeRepository.findAll().stream()
                .filter(e -> e.getAccount().getId().equals(account.getId()))
                .findFirst();

        if (employeeOpt.isPresent()) {
            Employee e = employeeOpt.get();
            response.setId(e.getId());
            response.setFullName(e.getFullName());
            response.setEmail(e.getEmail());
            response.setPhoneNumber(e.getPhoneNumber());
            response.setAddress("N/A"); // Nhân viên thường không lưu địa chỉ giao hàng
            return response;
        }

        throw new RuntimeException("Không tìm thấy hồ sơ chi tiết cho tài khoản này!");
    }

    private void updateCustomerInfo(Account account, UpdateProfileRequest request) {
        Customer customer = customerRepository.findAll().stream()
                .filter(c -> c.getAccount().getId().equals(account.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin khách hàng"));

        if (request.getFullName() != null) customer.setFullName(request.getFullName());
        if (request.getPhoneNumber() != null) customer.setPhoneNumber(request.getPhoneNumber());
        if (request.getAddress() != null) customer.setAddress(request.getAddress());
        if (request.getEmail() != null) customer.setEmail(request.getEmail());

        customerRepository.save(customer);
    }

    private void updateEmployeeInfo(Account account, UpdateProfileRequest request) {
        // Tìm Employee dựa trên Account
        Employee employee = employeeRepository.findAll().stream()
                .filter(e -> e.getAccount().getId().equals(account.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ nhân viên"));

        // Chỉ cho phép sửa Tên
        if (request.getFullName() != null) employee.setFullName(request.getFullName());


        employeeRepository.save(employee);
    }
}