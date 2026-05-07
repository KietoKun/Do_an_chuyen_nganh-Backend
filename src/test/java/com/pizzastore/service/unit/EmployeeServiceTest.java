package com.pizzastore.service.unit;

import com.pizzastore.dto.EmployeeResponse;
import com.pizzastore.entity.Account;
import com.pizzastore.entity.Branch;
import com.pizzastore.entity.Employee;
import com.pizzastore.enums.RoleName;
import com.pizzastore.repository.AccountRepository;
import com.pizzastore.repository.BranchRepository;
import com.pizzastore.repository.EmployeeRepository;
import com.pizzastore.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private EmployeeService employeeService;

    @Test
    void createEmployeeShouldRejectCustomerRole() {
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                employeeService.createEmployee(
                        "Employee",
                        "0900000001",
                        "HCM",
                        LocalDate.of(2000, 1, 1),
                        "e@example.com",
                        "Staff",
                        RoleName.CUSTOMER,
                        1L,
                        "admin"
                ));

        assertNotNull(ex.getMessage());
    }

    @Test
    void createEmployeeShouldRejectDuplicatePhone() {
        when(accountRepository.existsByUsername("0900000001")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                employeeService.createEmployee(
                        "Employee",
                        "0900000001",
                        "HCM",
                        LocalDate.of(2000, 1, 1),
                        "e@example.com",
                        "Staff",
                        RoleName.STAFF,
                        1L,
                        "admin"
                ));

        assertNotNull(ex.getMessage());
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void createEmployeeShouldCreateEmployeeForSuperAdmin() {
        Branch branch = branch(1L, "Branch A");
        Account creator = account(RoleName.SUPER_ADMIN, "admin");

        when(accountRepository.existsByUsername("0900000001")).thenReturn(false);
        when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(creator));
        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> "encoded-" + invocation.getArgument(0, String.class));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String message = employeeService.createEmployee(
                "Employee A",
                "0900000001",
                "HCM",
                LocalDate.of(2000, 1, 1),
                "e@example.com",
                "Staff",
                RoleName.STAFF,
                1L,
                "admin"
        );

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(captor.capture());
        Employee saved = captor.getValue();

        assertTrue(message.contains("0900000001"));
        assertEquals("0900000001", saved.getAccount().getUsername());
        assertEquals(RoleName.STAFF, saved.getAccount().getRole());
        assertEquals(branch, saved.getBranch());
        assertTrue(saved.getAccount().getPassword().startsWith("encoded-"));
        assertNotNull(saved.getFullName());
    }

    @Test
    void createEmployeeShouldThrowWhenSuperAdminDoesNotChooseBranch() {
        Account creator = account(RoleName.SUPER_ADMIN, "admin");

        when(accountRepository.existsByUsername("0900000001")).thenReturn(false);
        when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(creator));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                employeeService.createEmployee(
                        "Employee A",
                        "0900000001",
                        "HCM",
                        LocalDate.of(2000, 1, 1),
                        "e@example.com",
                        "Staff",
                        RoleName.STAFF,
                        null,
                        "admin"
                ));

        assertNotNull(ex.getMessage());
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void createEmployeeShouldCreateStaffInManagersOwnBranch() {
        Account managerAccount = account(RoleName.MANAGER, "manager-1");
        Branch managerBranch = branch(2L, "Branch B");
        Employee manager = new Employee();
        manager.setAccount(managerAccount);
        manager.setBranch(managerBranch);

        when(accountRepository.existsByUsername("0900000002")).thenReturn(false);
        when(accountRepository.findByUsername("manager-1")).thenReturn(Optional.of(managerAccount));
        when(employeeRepository.findByAccount_Username("manager-1")).thenReturn(Optional.of(manager));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        employeeService.createEmployee(
                "Employee B",
                "0900000002",
                "HCM",
                LocalDate.of(2000, 1, 1),
                "b@example.com",
                "Staff",
                RoleName.STAFF,
                99L,
                "manager-1"
        );

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(captor.capture());
        assertEquals(managerBranch, captor.getValue().getBranch());
    }

    @Test
    void createEmployeeShouldRejectManagerCreatingAnotherManager() {
        Account creatorAccount = account(RoleName.MANAGER, "manager-1");

        when(accountRepository.existsByUsername("0900000002")).thenReturn(false);
        when(accountRepository.findByUsername("manager-1")).thenReturn(Optional.of(creatorAccount));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                employeeService.createEmployee(
                        "Employee B",
                        "0900000002",
                        "HCM",
                        LocalDate.of(2000, 1, 1),
                        "b@example.com",
                        "Manager",
                        RoleName.MANAGER,
                        null,
                        "manager-1"
                ));

        assertTrue(ex.getMessage().contains("Manager"));
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void getAllEmployeesShouldRespectRoleScope() {
        Account superAdmin = account(RoleName.SUPER_ADMIN, "admin");
        Account managerAccount = account(RoleName.MANAGER, "manager-1");
        Employee manager = new Employee();
        manager.setAccount(managerAccount);
        manager.setBranch(branch(2L, "Branch B"));

        Employee e1 = employee(1L, "A", "0901", 2L, "Branch B");
        Employee e2 = employee(2L, "B", "0902", 3L, "Branch C");

        when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(superAdmin));
        when(employeeRepository.findAll()).thenReturn(List.of(e1, e2));

        List<EmployeeResponse> adminView = employeeService.getAllEmployees("admin");
        assertEquals(2, adminView.size());

        when(accountRepository.findByUsername("manager-1")).thenReturn(Optional.of(managerAccount));
        when(employeeRepository.findByAccount_Username("manager-1")).thenReturn(Optional.of(manager));
        when(employeeRepository.findByBranch_Id(2L)).thenReturn(List.of(e1));

        List<EmployeeResponse> managerView = employeeService.getAllEmployees("manager-1");
        assertEquals(1, managerView.size());
        assertEquals("A", managerView.get(0).getFullName());
    }

    @Test
    void deleteEmployeeShouldRejectManagerOutsideBranch() {
        Account managerAccount = account(RoleName.MANAGER, "manager-1");
        Employee manager = new Employee();
        manager.setAccount(managerAccount);
        manager.setBranch(branch(2L, "Branch B"));

        Employee target = employee(10L, "Target", "0999", 3L, "Branch C");

        when(employeeRepository.findById(10L)).thenReturn(Optional.of(target));
        when(accountRepository.findByUsername("manager-1")).thenReturn(Optional.of(managerAccount));
        when(employeeRepository.findByAccount_Username("manager-1")).thenReturn(Optional.of(manager));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> employeeService.deleteEmployee(10L, "manager-1"));

        assertNotNull(ex.getMessage());
        verify(employeeRepository, never()).deleteById(10L);
    }

    @Test
    void deleteEmployeeShouldDeleteWhenExistingIdIsUsedWithoutScope() {
        when(employeeRepository.existsById(10L)).thenReturn(true);

        employeeService.deleteEmployee(10L);

        verify(employeeRepository).deleteById(10L);
    }

    @Test
    void deleteEmployeeShouldThrowWhenIdIsMissingWithoutScope() {
        when(employeeRepository.existsById(10L)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> employeeService.deleteEmployee(10L));

        assertNotNull(ex.getMessage());
        verify(employeeRepository, never()).deleteById(10L);
    }

    @Test
    void deleteEmployeeShouldRejectManagerDeletingAnotherManager() {
        Account managerAccount = account(RoleName.MANAGER, "manager-1");
        Employee manager = new Employee();
        manager.setAccount(managerAccount);
        manager.setBranch(branch(2L, "Branch B"));

        Employee target = employee(10L, "Target Manager", "0999", 2L, "Branch B");
        target.setAccount(account(RoleName.MANAGER, "target-manager"));

        when(employeeRepository.findById(10L)).thenReturn(Optional.of(target));
        when(accountRepository.findByUsername("manager-1")).thenReturn(Optional.of(managerAccount));
        when(employeeRepository.findByAccount_Username("manager-1")).thenReturn(Optional.of(manager));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> employeeService.deleteEmployee(10L, "manager-1"));

        assertNotNull(ex.getMessage());
        verify(employeeRepository, never()).deleteById(10L);
    }

    @Test
    void checkInEmployeeShouldUpdateLastCheckIn() {
        Employee employee = employee(1L, "Clock In", "0901", 2L, "Branch B");

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String message = employeeService.checkInEmployee(1L);

        assertNotNull(message);
        assertNotNull(employee.getLastCheckIn());
        verify(employeeRepository).save(employee);
    }

    @Test
    void checkInEmployeeShouldThrowWhenEmployeeIsMissing() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> employeeService.checkInEmployee(1L));

        assertNotNull(ex.getMessage());
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void getEmployeeByIdShouldReturnMappedResponse() {
        Employee employee = employee(1L, "Employee A", "0901", 2L, "Branch B");
        employee.setAddress("HCM");
        employee.setEmail("a@example.com");
        employee.setPosition("Staff");

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        EmployeeResponse response = employeeService.getEmployeeById(1L);

        assertEquals(1L, response.getId());
        assertEquals("Employee A", response.getFullName());
        assertEquals(2L, response.getBranchId());
        assertEquals("Branch B", response.getBranchName());
    }

    @Test
    void getEmployeeByIdShouldThrowWhenEmployeeIsMissing() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> employeeService.getEmployeeById(1L));

        assertNotNull(ex.getMessage());
    }

    @Test
    void getEmployeeByIdWithUsernameShouldAllowSuperAdmin() {
        Account admin = account(RoleName.SUPER_ADMIN, "admin");
        Employee employee = employee(1L, "Employee A", "0901", 2L, "Branch B");

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        EmployeeResponse response = employeeService.getEmployeeById(1L, "admin");

        assertEquals("Employee A", response.getFullName());
    }

    @Test
    void getEmployeeByIdWithUsernameShouldRejectManagerOutsideBranch() {
        Account managerAccount = account(RoleName.MANAGER, "manager-1");
        Employee manager = new Employee();
        manager.setAccount(managerAccount);
        manager.setBranch(branch(2L, "Branch B"));
        Employee target = employee(1L, "Employee A", "0901", 3L, "Branch C");

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(target));
        when(accountRepository.findByUsername("manager-1")).thenReturn(Optional.of(managerAccount));
        when(employeeRepository.findByAccount_Username("manager-1")).thenReturn(Optional.of(manager));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> employeeService.getEmployeeById(1L, "manager-1"));

        assertNotNull(ex.getMessage());
    }

    private Account account(RoleName role, String username) {
        Account account = new Account();
        account.setUsername(username);
        account.setPassword("secret");
        account.setRole(role);
        return account;
    }

    private Branch branch(Long id, String name) {
        Branch branch = new Branch();
        branch.setId(id);
        branch.setName(name);
        return branch;
    }

    private Employee employee(Long id, String fullName, String phone, Long branchId, String branchName) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setFullName(fullName);
        employee.setPhoneNumber(phone);
        employee.setAccount(account(RoleName.STAFF, phone));
        employee.setBranch(branch(branchId, branchName));
        return employee;
    }
}
