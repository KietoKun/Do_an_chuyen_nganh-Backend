package com.pizzastore.service.unit;

import com.pizzastore.entity.Account;
import com.pizzastore.entity.Branch;
import com.pizzastore.entity.Employee;
import com.pizzastore.entity.Order;
import com.pizzastore.enums.RoleName;
import com.pizzastore.repository.AccountRepository;
import com.pizzastore.repository.EmployeeRepository;
import com.pizzastore.service.BranchAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BranchAccessServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private BranchAccessService branchAccessService;

    @Test
    void resolveVisibleBranchIdShouldReturnRequestedBranchForSuperAdmin() {
        Account account = account(RoleName.SUPER_ADMIN);
        when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(account));

        Long branchId = branchAccessService.resolveVisibleBranchId("admin", 9L);

        assertEquals(9L, branchId);
    }

    @Test
    void resolveVisibleBranchIdShouldReturnEmployeeBranchForManager() {
        Account account = account(RoleName.MANAGER);
        Employee employee = employee("manager", 3L);
        when(accountRepository.findByUsername("manager")).thenReturn(Optional.of(account));
        when(employeeRepository.findByAccount_Username("manager")).thenReturn(Optional.of(employee));

        Long branchId = branchAccessService.resolveVisibleBranchId("manager", 3L);

        assertEquals(3L, branchId);
    }

    @Test
    void resolveVisibleBranchIdShouldThrowWhenManagerRequestsOtherBranch() {
        Account account = account(RoleName.MANAGER);
        Employee employee = employee("manager", 3L);
        when(accountRepository.findByUsername("manager")).thenReturn(Optional.of(account));
        when(employeeRepository.findByAccount_Username("manager")).thenReturn(Optional.of(employee));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> branchAccessService.resolveVisibleBranchId("manager", 7L));

        assertEquals("Ban chi duoc truy cap du lieu cua chi nhanh minh", ex.getMessage());
    }

    @Test
    void getAccountShouldThrowWhenMissing() {
        when(accountRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> branchAccessService.getAccount("ghost"));

        assertEquals("Khong tim thay tai khoan", ex.getMessage());
    }

    @Test
    void getEmployeeShouldThrowWhenMissing() {
        when(employeeRepository.findByAccount_Username("ghost")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> branchAccessService.getEmployee("ghost"));

        assertEquals("Khong tim thay ho so nhan vien", ex.getMessage());
    }

    @Test
    void assertCanAccessBranchShouldAllowSuperAdmin() {
        Account account = account(RoleName.SUPER_ADMIN);
        when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(account));

        branchAccessService.assertCanAccessBranch("admin", branch(1L));

        verify(accountRepository).findByUsername("admin");
    }

    @Test
    void assertCanAccessBranchShouldThrowWhenBranchIsMissingForManager() {
        Account account = account(RoleName.MANAGER);
        when(accountRepository.findByUsername("manager")).thenReturn(Optional.of(account));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> branchAccessService.assertCanAccessBranch("manager", null));

        assertEquals("Du lieu chua duoc gan chi nhanh", ex.getMessage());
    }

    @Test
    void assertCanAccessOrderShouldAllowManagerForOwnBranch() {
        Account account = account(RoleName.MANAGER);
        Employee employee = employee("manager", 5L);
        when(accountRepository.findByUsername("manager")).thenReturn(Optional.of(account));
        when(employeeRepository.findByAccount_Username("manager")).thenReturn(Optional.of(employee));

        Order order = new Order();
        order.setBranch(branch(5L));

        branchAccessService.assertCanAccessOrder("manager", order);
    }

    @Test
    void assertCanAccessOrderShouldThrowWhenOrderBranchDiffers() {
        Account account = account(RoleName.MANAGER);
        Employee employee = employee("manager", 5L);
        when(accountRepository.findByUsername("manager")).thenReturn(Optional.of(account));
        when(employeeRepository.findByAccount_Username("manager")).thenReturn(Optional.of(employee));

        Order order = new Order();
        order.setBranch(branch(8L));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> branchAccessService.assertCanAccessOrder("manager", order));

        assertEquals("Ban chi duoc truy cap du lieu cua chi nhanh minh", ex.getMessage());
    }

    private Account account(RoleName role) {
        Account account = new Account();
        account.setUsername(role.name().toLowerCase());
        account.setPassword("secret");
        account.setRole(role);
        return account;
    }

    private Employee employee(String username, Long branchId) {
        Account account = new Account();
        account.setUsername(username);
        account.setPassword("secret");
        account.setRole(RoleName.MANAGER);

        Employee employee = new Employee();
        employee.setAccount(account);
        employee.setBranch(branch(branchId));
        return employee;
    }

    private Branch branch(Long id) {
        Branch branch = new Branch();
        branch.setId(id);
        return branch;
    }
}
