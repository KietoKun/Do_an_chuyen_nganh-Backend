package com.pizzastore.service;

import com.pizzastore.entity.Account;
import com.pizzastore.entity.Branch;
import com.pizzastore.entity.Employee;
import com.pizzastore.entity.Order;
import com.pizzastore.enums.RoleName;
import com.pizzastore.repository.AccountRepository;
import com.pizzastore.repository.EmployeeRepository;
import org.springframework.stereotype.Service;

@Service
public class BranchAccessService {
    private final AccountRepository accountRepository;
    private final EmployeeRepository employeeRepository;

    public BranchAccessService(AccountRepository accountRepository, EmployeeRepository employeeRepository) {
        this.accountRepository = accountRepository;
        this.employeeRepository = employeeRepository;
    }

    public Account getAccount(String username) {
        return accountRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Khong tim thay tai khoan"));
    }

    public Employee getEmployee(String username) {
        return employeeRepository.findByAccount_Username(username)
                .orElseThrow(() -> new RuntimeException("Khong tim thay ho so nhan vien"));
    }

    public Long resolveVisibleBranchId(String username, Long requestedBranchId) {
        Account account = getAccount(username);
        if (account.getRole() == RoleName.SUPER_ADMIN) {
            return requestedBranchId;
        }

        Employee employee = getEmployee(username);
        Long employeeBranchId = getEmployeeBranchId(employee);
        if (requestedBranchId != null && !employeeBranchId.equals(requestedBranchId)) {
            throw new RuntimeException("Ban chi duoc truy cap du lieu cua chi nhanh minh");
        }
        return employeeBranchId;
    }

    public void assertCanAccessBranch(String username, Branch branch) {
        Account account = getAccount(username);
        if (account.getRole() == RoleName.SUPER_ADMIN) {
            return;
        }
        if (branch == null || branch.getId() == null) {
            throw new RuntimeException("Du lieu chua duoc gan chi nhanh");
        }

        Long employeeBranchId = getEmployeeBranchId(getEmployee(username));
        if (!employeeBranchId.equals(branch.getId())) {
            throw new RuntimeException("Ban chi duoc truy cap du lieu cua chi nhanh minh");
        }
    }

    public void assertCanAccessOrder(String username, Order order) {
        assertCanAccessBranch(username, order.getBranch());
    }

    private Long getEmployeeBranchId(Employee employee) {
        if (employee.getBranch() == null || employee.getBranch().getId() == null) {
            throw new RuntimeException("Nhan vien chua duoc gan chi nhanh");
        }
        return employee.getBranch().getId();
    }
}
