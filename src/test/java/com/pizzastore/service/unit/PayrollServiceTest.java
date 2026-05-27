package com.pizzastore.service.unit;

import com.pizzastore.dto.PayrollRecordResponse;
import com.pizzastore.dto.PayrollSummaryResponse;
import com.pizzastore.entity.Account;
import com.pizzastore.entity.AttendanceRecord;
import com.pizzastore.entity.Branch;
import com.pizzastore.entity.Employee;
import com.pizzastore.entity.PayrollRecord;
import com.pizzastore.enums.AttendanceStatus;
import com.pizzastore.enums.PayrollStatus;
import com.pizzastore.enums.RoleName;
import com.pizzastore.repository.BranchRepository;
import com.pizzastore.repository.EmployeeRepository;
import com.pizzastore.repository.PayrollRecordRepository;
import com.pizzastore.service.AttendanceService;
import com.pizzastore.service.BranchAccessService;
import com.pizzastore.service.PayrollService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayrollServiceTest {

    @Mock
    private PayrollRecordRepository payrollRecordRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private AttendanceService attendanceService;

    @Mock
    private BranchAccessService branchAccessService;

    @InjectMocks
    private PayrollService payrollService;

    @Test
    void calculateEmployeePayrollShouldSumClosedAttendanceRecords() {
        Branch branch = branch(1L, "Branch A");
        Employee employee = employee(10L, "Staff A", RoleName.STAFF, branch, 50000.0);
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 31);

        when(employeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(branchAccessService.getAccount("admin")).thenReturn(account(RoleName.SUPER_ADMIN));
        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(attendanceService.getClosedRecordsForEmployee(10L, from, to)).thenReturn(List.of(
                attendance(employee, 480L),
                attendance(employee, 240L)
        ));

        PayrollSummaryResponse response = payrollService.calculateEmployeePayroll(10L, from, to, "admin");

        assertEquals(720L, response.getTotalWorkMinutes());
        assertEquals(12.0, response.getTotalWorkHours());
        assertEquals(600000.0, response.getGrossSalary());
        assertEquals(600000.0, response.getNetSalary());
    }

    @Test
    void calculateEmployeePayrollShouldRejectInvalidPeriod() {
        Employee employee = employee(10L, "Staff A", RoleName.STAFF, branch(1L, "Branch A"), 50000.0);

        when(employeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(branchAccessService.getAccount("admin")).thenReturn(account(RoleName.SUPER_ADMIN));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> payrollService.calculateEmployeePayroll(
                10L,
                LocalDate.of(2026, 5, 31),
                LocalDate.of(2026, 5, 1),
                "admin"
        ));

        assertNotNull(ex.getMessage());
    }

    @Test
    void calculateEmployeePayrollShouldLetStaffViewOnlyOwnPayroll() {
        Branch branch = branch(1L, "Branch A");
        Employee target = employee(10L, "Staff A", RoleName.STAFF, branch, 50000.0);
        Employee requester = employee(11L, "Staff B", RoleName.STAFF, branch, 50000.0);

        when(employeeRepository.findById(10L)).thenReturn(Optional.of(target));
        when(branchAccessService.getAccount("staff-b")).thenReturn(account(RoleName.STAFF));
        when(branchAccessService.getEmployee("staff-b")).thenReturn(requester);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> payrollService.calculateEmployeePayroll(
                10L,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                "staff-b"
        ));

        assertNotNull(ex.getMessage());
    }

    @Test
    void generatePayrollShouldCreateDraftsForVisibleBranchEmployees() {
        Branch branch = branch(1L, "Branch A");
        Employee staff = employee(10L, "Staff A", RoleName.STAFF, branch, 50000.0);
        Employee chef = employee(11L, "Chef A", RoleName.CHEF, branch, 60000.0);
        Employee customerRole = employee(12L, "Customer Role", RoleName.CUSTOMER, branch, 60000.0);
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 31);

        when(branchAccessService.resolveVisibleBranchId("manager", 1L)).thenReturn(1L);
        when(employeeRepository.findByBranch_Id(1L)).thenReturn(List.of(staff, chef, customerRole));
        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(payrollRecordRepository.findByEmployee_IdAndPeriodStartAndPeriodEnd(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(attendanceService.getClosedRecordsForEmployee(10L, from, to)).thenReturn(List.of(attendance(staff, 480L)));
        when(attendanceService.getClosedRecordsForEmployee(11L, from, to)).thenReturn(List.of(attendance(chef, 60L)));
        when(payrollRecordRepository.save(any(PayrollRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        List<PayrollRecordResponse> responses = payrollService.generatePayroll(1L, from, to, "manager");

        assertEquals(2, responses.size());
        assertEquals(PayrollStatus.DRAFT, responses.get(0).getStatus());
        verify(payrollRecordRepository, times(2)).save(any(PayrollRecord.class));
    }

    @Test
    void generatePayrollShouldUpdateExistingDraftInsteadOfCreatingDuplicate() {
        Branch branch = branch(1L, "Branch A");
        Employee staff = employee(10L, "Staff A", RoleName.STAFF, branch, 50000.0);
        PayrollRecord existing = payrollRecord(50L, staff, branch, PayrollStatus.DRAFT);
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 31);

        when(branchAccessService.resolveVisibleBranchId("manager", 1L)).thenReturn(1L);
        when(employeeRepository.findByBranch_Id(1L)).thenReturn(List.of(staff));
        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(payrollRecordRepository.findByEmployee_IdAndPeriodStartAndPeriodEnd(10L, from, to))
                .thenReturn(Optional.of(existing));
        when(attendanceService.getClosedRecordsForEmployee(10L, from, to)).thenReturn(List.of(attendance(staff, 120L)));
        when(payrollRecordRepository.save(existing)).thenReturn(existing);

        List<PayrollRecordResponse> responses = payrollService.generatePayroll(1L, from, to, "manager");

        assertEquals(1, responses.size());
        assertEquals(50L, responses.get(0).getId());
        assertEquals(120L, existing.getTotalWorkMinutes());
        assertEquals(PayrollStatus.DRAFT, existing.getStatus());
    }

    @Test
    void generatePayrollShouldKeepPaidRecordUnchanged() {
        Branch branch = branch(1L, "Branch A");
        Employee staff = employee(10L, "Staff A", RoleName.STAFF, branch, 50000.0);
        PayrollRecord paid = payrollRecord(50L, staff, branch, PayrollStatus.PAID);
        paid.setTotalWorkMinutes(60L);
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 31);

        when(branchAccessService.resolveVisibleBranchId("manager", 1L)).thenReturn(1L);
        when(employeeRepository.findByBranch_Id(1L)).thenReturn(List.of(staff));
        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(payrollRecordRepository.findByEmployee_IdAndPeriodStartAndPeriodEnd(10L, from, to))
                .thenReturn(Optional.of(paid));
        when(attendanceService.getClosedRecordsForEmployee(10L, from, to)).thenReturn(List.of(attendance(staff, 480L)));

        List<PayrollRecordResponse> responses = payrollService.generatePayroll(1L, from, to, "manager");

        assertEquals(PayrollStatus.PAID, responses.get(0).getStatus());
        assertEquals(60L, responses.get(0).getTotalWorkMinutes());
        verify(payrollRecordRepository, never()).save(any());
    }

    @Test
    void confirmPayrollShouldMoveDraftToConfirmed() {
        Branch branch = branch(1L, "Branch A");
        PayrollRecord record = payrollRecord(50L, employee(10L, "Staff A", RoleName.STAFF, branch, 50000.0),
                branch, PayrollStatus.DRAFT);

        when(payrollRecordRepository.findById(50L)).thenReturn(Optional.of(record));
        when(branchAccessService.getAccount("admin")).thenReturn(account(RoleName.SUPER_ADMIN));
        when(payrollRecordRepository.save(record)).thenReturn(record);

        PayrollRecordResponse response = payrollService.confirmPayroll(50L, "admin");

        assertEquals(PayrollStatus.CONFIRMED, response.getStatus());
        assertNotNull(record.getConfirmedAt());
    }

    @Test
    void confirmPayrollShouldRejectPaidRecord() {
        Branch branch = branch(1L, "Branch A");
        PayrollRecord record = payrollRecord(50L, employee(10L, "Staff A", RoleName.STAFF, branch, 50000.0),
                branch, PayrollStatus.PAID);

        when(payrollRecordRepository.findById(50L)).thenReturn(Optional.of(record));
        when(branchAccessService.getAccount("admin")).thenReturn(account(RoleName.SUPER_ADMIN));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> payrollService.confirmPayroll(50L, "admin"));

        assertNotNull(ex.getMessage());
        verify(payrollRecordRepository, never()).save(any());
    }

    @Test
    void markPaidShouldMoveConfirmedToPaid() {
        Branch branch = branch(1L, "Branch A");
        PayrollRecord record = payrollRecord(50L, employee(10L, "Staff A", RoleName.STAFF, branch, 50000.0),
                branch, PayrollStatus.CONFIRMED);

        when(payrollRecordRepository.findById(50L)).thenReturn(Optional.of(record));
        when(branchAccessService.getAccount("admin")).thenReturn(account(RoleName.SUPER_ADMIN));
        when(payrollRecordRepository.save(record)).thenReturn(record);

        PayrollRecordResponse response = payrollService.markPaid(50L, "admin");

        assertEquals(PayrollStatus.PAID, response.getStatus());
        assertNotNull(record.getPaidAt());
    }

    @Test
    void markPaidShouldRejectDraftRecord() {
        Branch branch = branch(1L, "Branch A");
        PayrollRecord record = payrollRecord(50L, employee(10L, "Staff A", RoleName.STAFF, branch, 50000.0),
                branch, PayrollStatus.DRAFT);

        when(payrollRecordRepository.findById(50L)).thenReturn(Optional.of(record));
        when(branchAccessService.getAccount("admin")).thenReturn(account(RoleName.SUPER_ADMIN));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> payrollService.markPaid(50L, "admin"));

        assertNotNull(ex.getMessage());
        verify(payrollRecordRepository, never()).save(any());
    }

    @Test
    void managerActionsShouldUseBranchAccessForRecordBranch() {
        Branch branch = branch(1L, "Branch A");
        PayrollRecord record = payrollRecord(50L, employee(10L, "Staff A", RoleName.STAFF, branch, 50000.0),
                branch, PayrollStatus.CONFIRMED);

        when(payrollRecordRepository.findById(50L)).thenReturn(Optional.of(record));
        when(branchAccessService.getAccount("manager")).thenReturn(account(RoleName.MANAGER));
        when(payrollRecordRepository.save(record)).thenReturn(record);

        payrollService.markPaid(50L, "manager");

        verify(branchAccessService).assertCanAccessBranch("manager", branch);
    }

    @Test
    void getPayrollRecordsShouldSortByBranchThenEmployee() {
        Branch branchB = branch(2L, "Branch B");
        Branch branchA = branch(1L, "Branch A");
        PayrollRecord b = payrollRecord(1L, employee(1L, "Beta", RoleName.STAFF, branchB, 1.0),
                branchB, PayrollStatus.DRAFT);
        PayrollRecord a2 = payrollRecord(2L, employee(2L, "Zulu", RoleName.STAFF, branchA, 1.0),
                branchA, PayrollStatus.DRAFT);
        PayrollRecord a1 = payrollRecord(3L, employee(3L, "Alpha", RoleName.STAFF, branchA, 1.0),
                branchA, PayrollStatus.DRAFT);
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 31);

        when(branchAccessService.resolveVisibleBranchId("admin", null)).thenReturn(null);
        when(payrollRecordRepository.findByPeriodStartAndPeriodEnd(from, to)).thenReturn(List.of(b, a2, a1));

        List<PayrollRecordResponse> responses = payrollService.getPayrollRecords(null, from, to, "admin");

        assertEquals("Alpha", responses.get(0).getEmployeeName());
        assertEquals("Zulu", responses.get(1).getEmployeeName());
        assertEquals("Beta", responses.get(2).getEmployeeName());
    }

    @Test
    void generatePayrollShouldSaveCalculatedDraftValues() {
        Branch branch = branch(1L, "Branch A");
        Employee staff = employee(10L, "Staff A", RoleName.STAFF, branch, 50000.0);
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 31);

        when(branchAccessService.resolveVisibleBranchId("manager", 1L)).thenReturn(1L);
        when(employeeRepository.findByBranch_Id(1L)).thenReturn(List.of(staff));
        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(payrollRecordRepository.findByEmployee_IdAndPeriodStartAndPeriodEnd(10L, from, to))
                .thenReturn(Optional.empty());
        when(attendanceService.getClosedRecordsForEmployee(10L, from, to)).thenReturn(List.of(attendance(staff, 90L)));
        when(payrollRecordRepository.save(any(PayrollRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        payrollService.generatePayroll(1L, from, to, "manager");

        ArgumentCaptor<PayrollRecord> captor = ArgumentCaptor.forClass(PayrollRecord.class);
        verify(payrollRecordRepository).save(captor.capture());
        PayrollRecord saved = captor.getValue();

        assertEquals(90L, saved.getTotalWorkMinutes());
        assertEquals(75000.0, saved.getGrossSalary());
        assertEquals(75000.0, saved.getNetSalary());
        assertEquals(PayrollStatus.DRAFT, saved.getStatus());
        assertNotNull(saved.getCreatedAt());
    }

    private Account account(RoleName role) {
        Account account = new Account();
        account.setRole(role);
        return account;
    }

    private Branch branch(Long id, String name) {
        Branch branch = new Branch();
        branch.setId(id);
        branch.setName(name);
        return branch;
    }

    private Employee employee(Long id, String fullName, RoleName role, Branch branch, Double salaryPerHour) {
        Account account = account(role);
        account.setUsername(fullName.toLowerCase().replace(" ", "-"));

        Employee employee = new Employee();
        employee.setId(id);
        employee.setFullName(fullName);
        employee.setBranch(branch);
        employee.setAccount(account);
        employee.setSalaryPerHour(salaryPerHour);
        return employee;
    }

    private AttendanceRecord attendance(Employee employee, Long workMinutes) {
        AttendanceRecord record = new AttendanceRecord();
        record.setEmployee(employee);
        record.setBranch(employee.getBranch());
        record.setCheckInTime(LocalDateTime.of(2026, 5, 1, 9, 0));
        record.setCheckOutTime(LocalDateTime.of(2026, 5, 1, 17, 0));
        record.setWorkMinutes(workMinutes);
        record.setStatus(AttendanceStatus.CHECKED_OUT);
        record.setCreatedAt(LocalDateTime.of(2026, 5, 1, 9, 0));
        return record;
    }

    private PayrollRecord payrollRecord(Long id, Employee employee, Branch branch, PayrollStatus status) {
        PayrollRecord record = new PayrollRecord();
        record.setId(id);
        record.setEmployee(employee);
        record.setBranch(branch);
        record.setPeriodStart(LocalDate.of(2026, 5, 1));
        record.setPeriodEnd(LocalDate.of(2026, 5, 31));
        record.setSalaryPerHour(employee.getSalaryPerHour());
        record.setTotalWorkMinutes(480L);
        record.setGrossSalary(400000.0);
        record.setBonus(0.0);
        record.setDeduction(0.0);
        record.setNetSalary(400000.0);
        record.setStatus(status);
        record.setCreatedAt(LocalDateTime.of(2026, 5, 31, 12, 0));
        return record;
    }
}
