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

        // --- QUAN TRỌNG: BỎ ĐOẠN ĐỔI USERNAME ĐI ---
        // Chúng ta KHÔNG cho phép đổi username nữa.
        // Nếu nhân viên đổi số điện thoại, Manager phải tạo tài khoản mới hoặc dùng quy trình Admin riêng.

        /* ĐOẠN NÀY BỎ HOẶC COMMENT LẠI
        if (request.getUsername() != null && ...) {
            account.setUsername(request.getUsername());
        }
        */

        // 2. Cập nhật thông tin cá nhân (Tên, Địa chỉ, Email...)
        if (account.getRole() == RoleName.CUSTOMER) {
            updateCustomerInfo(account, request);
        } else {
            updateEmployeeInfo(account, request);
        }
    }

    private void updateCustomerInfo(Account account, UpdateProfileRequest request) {
        // Tìm Customer dựa trên Account ID (Logic này tùy thuộc vào cách bạn map DB)
        // Cách đơn giản nhất: Query Customer có account_id = account.getId()
        // Giả sử bạn đã có hàm tìm kiếm này, hoặc Account có link sang Customer (OneToOne mappedBy)

        // *Lưu ý*: Để đơn giản, ta tìm Customer theo Account
        // Bạn cần thêm hàm findByAccount trong CustomerRepository nếu chưa có
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
        // (Lưu ý: Vì Employee giờ kế thừa Person, bạn có thể phải tìm trong EmployeeRepository)
        Employee employee = employeeRepository.findAll().stream()
                .filter(e -> e.getAccount().getId().equals(account.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ nhân viên"));

        // Chỉ cho phép sửa Tên
        if (request.getFullName() != null) employee.setFullName(request.getFullName());

        // --- CÒN SỐ ĐIỆN THOẠI THÌ SAO? ---
        // Vì SĐT giờ gắn liền với Username (để đăng nhập),
        // nên nếu bạn cho đổi SĐT ở đây -> Bạn phải code thêm logic cập nhật lại username bên bảng Account.
        // Nhưng theo yêu cầu "không cho đổi", ta sẽ COMMENT dòng này lại:

        // if (request.getPhoneNumber() != null) employee.setPhoneNumber(request.getPhoneNumber());

        employeeRepository.save(employee);
    }
}