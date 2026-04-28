package com.pizzastore.service;

import com.pizzastore.dto.EmployeeResponse;
import com.pizzastore.entity.Account;
import com.pizzastore.entity.Branch;
import com.pizzastore.entity.Employee;
import com.pizzastore.enums.RoleName;
import com.pizzastore.repository.AccountRepository;
import com.pizzastore.repository.BranchRepository;
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
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public EmployeeService(EmployeeRepository employeeRepository,
                           AccountRepository accountRepository,
                           BranchRepository branchRepository,
                           PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.accountRepository = accountRepository;
        this.branchRepository = branchRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public String createEmployee(String fullName, String phone, String address, java.time.LocalDate dateOfBirth,
                                 String email, String position, RoleName role, Long branchId, String creatorUsername) {
        if (role == RoleName.CUSTOMER || role == RoleName.SUPER_ADMIN) {
            throw new RuntimeException("Không thể tạo nhân viên với quyền " + role);
        }

        if (accountRepository.existsByUsername(phone)) {
            throw new RuntimeException("Số điện thoại (tài khoản) này đã tồn tại");
        }

        Account creatorAccount = accountRepository.findByUsername(creatorUsername)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản người tạo"));

        Branch assignedBranch = resolveAssignedBranch(creatorAccount, creatorUsername, role, branchId);
        String rawPassword = UUID.randomUUID().toString().substring(0, 6);

        Account account = new Account();
        account.setUsername(phone);
        account.setPassword(passwordEncoder.encode(rawPassword));
        account.setRole(role);
        account.setFirstLogin(true);

        Employee employee = new Employee();
        employee.setFullName(fullName);
        employee.setPhoneNumber(phone);
        employee.setAddress(address);
        employee.setDateOfBirth(dateOfBirth);
        employee.setEmail(email);
        employee.setPosition(position);
        employee.setBranch(assignedBranch);
        employee.setAccount(account);

        employeeRepository.save(employee);

        return "Tạo thành công! Tài khoản (SĐT): " + phone + " | Mật khẩu: " + rawPassword;
    }

    private Branch resolveAssignedBranch(Account creatorAccount, String creatorUsername, RoleName newRole, Long requestedBranchId) {
        if (creatorAccount.getRole() == RoleName.SUPER_ADMIN) {
            if (requestedBranchId == null) {
                throw new RuntimeException("SUPER_ADMIN phải chọn chi nhánh khi tạo quản lý/nhân viên");
            }
            return branchRepository.findById(requestedBranchId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh ID: " + requestedBranchId));
        }

        if (creatorAccount.getRole() == RoleName.MANAGER) {
            if (newRole == RoleName.MANAGER) {
                throw new RuntimeException("Manager chi nhánh không được tạo Manager khác");
            }
            Employee creator = employeeRepository.findByAccount_Username(creatorUsername)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ quản lý"));
            if (creator.getBranch() == null) {
                throw new RuntimeException("Quản lý hiện tại chưa được gán chi nhánh");
            }
            return creator.getBranch();
        }

        throw new RuntimeException("Chỉ SUPER_ADMIN hoặc MANAGER mới được tạo nhân viên");
    }

    public List<EmployeeResponse> getAllEmployees() {
        return employeeRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<EmployeeResponse> getAllEmployees(String username) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Khong tim thay tai khoan"));
        List<Employee> employees = account.getRole() == RoleName.SUPER_ADMIN
                ? employeeRepository.findAll()
                : employeeRepository.findByBranch_Id(resolveManagerBranchId(username));

        return employees.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public void deleteEmployee(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy nhân viên ID: " + id);
        }
        employeeRepository.deleteById(id);
    }

    public void deleteEmployee(Long id, String username) {
        Employee target = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Khong tim thay nhan vien ID: " + id));
        assertCanManageEmployee(username, target, true);
        employeeRepository.deleteById(id);
    }

    public String checkInEmployee(Long employeeId) {
        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Nhân viên không tồn tại"));

        emp.clockIn();
        employeeRepository.save(emp);

        return "Chấm công thành công cho: " + emp.getFullName() + " vào lúc " + emp.getLastCheckIn();
    }

    public EmployeeResponse getEmployeeById(Long id) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên với ID: " + id));
        return toResponse(emp);
    }

    public EmployeeResponse getEmployeeById(Long id, String username) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Khong tim thay nhan vien voi ID: " + id));
        assertCanManageEmployee(username, emp, false);
        return toResponse(emp);
    }

    private void assertCanManageEmployee(String username, Employee target, boolean deleting) {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Khong tim thay tai khoan"));
        if (account.getRole() == RoleName.SUPER_ADMIN) {
            return;
        }

        Long managerBranchId = resolveManagerBranchId(username);
        Long targetBranchId = target.getBranch() != null ? target.getBranch().getId() : null;
        if (targetBranchId == null || !managerBranchId.equals(targetBranchId)) {
            throw new RuntimeException("Manager chi duoc thao tac nhan vien cua chi nhanh minh");
        }
        if (deleting && target.getAccount() != null
                && (target.getAccount().getRole() == RoleName.MANAGER
                || target.getAccount().getRole() == RoleName.SUPER_ADMIN)) {
            throw new RuntimeException("Manager khong duoc xoa Manager hoac Super Admin");
        }
    }

    private Long resolveManagerBranchId(String username) {
        Employee manager = employeeRepository.findByAccount_Username(username)
                .orElseThrow(() -> new RuntimeException("Khong tim thay ho so quan ly"));
        if (manager.getBranch() == null || manager.getBranch().getId() == null) {
            throw new RuntimeException("Quan ly chua duoc gan chi nhanh");
        }
        return manager.getBranch().getId();
    }

    private EmployeeResponse toResponse(Employee emp) {
        return new EmployeeResponse(
                emp.getId(),
                emp.getFullName(),
                emp.getPhoneNumber(),
                emp.getAddress(),
                emp.getDateOfBirth(),
                emp.getEmail(),
                emp.getPosition(),
                emp.getAccount().getUsername(),
                emp.getAccount().getRole().name(),
                emp.getBranch() != null ? emp.getBranch().getId() : null,
                emp.getBranch() != null ? emp.getBranch().getName() : null
        );
    }
}
