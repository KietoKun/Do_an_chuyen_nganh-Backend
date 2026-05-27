package com.pizzastore.service.unit;

import com.pizzastore.dto.AttendanceRecordResponse;
import com.pizzastore.entity.Account;
import com.pizzastore.entity.AttendanceRecord;
import com.pizzastore.entity.Branch;
import com.pizzastore.entity.Employee;
import com.pizzastore.enums.AttendanceStatus;
import com.pizzastore.enums.RoleName;
import com.pizzastore.repository.AttendanceRecordRepository;
import com.pizzastore.repository.EmployeeRepository;
import com.pizzastore.service.AttendanceService;
import com.pizzastore.service.BranchAccessService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private BranchAccessService branchAccessService;

    @InjectMocks
    private AttendanceService attendanceService;

    @Test
    void checkInShouldCreateCheckedInRecordAndUpdateEmployee() {
        Employee employee = employee(1L, "Staff A", branch(10L, "Branch A"));

        when(branchAccessService.getEmployee("staff")).thenReturn(employee);
        when(attendanceRecordRepository.findFirstByEmployee_IdAndStatusOrderByCheckInTimeDesc(
                1L, AttendanceStatus.CHECKED_IN)).thenReturn(Optional.empty());
        when(attendanceRecordRepository.save(any(AttendanceRecord.class))).thenAnswer(invocation -> {
            AttendanceRecord record = invocation.getArgument(0);
            record.setId(99L);
            return record;
        });

        AttendanceRecordResponse response = attendanceService.checkIn("staff");

        ArgumentCaptor<AttendanceRecord> captor = ArgumentCaptor.forClass(AttendanceRecord.class);
        verify(attendanceRecordRepository).save(captor.capture());
        AttendanceRecord saved = captor.getValue();

        assertEquals(99L, response.getId());
        assertEquals(AttendanceStatus.CHECKED_IN, saved.getStatus());
        assertEquals(employee, saved.getEmployee());
        assertEquals(employee.getBranch(), saved.getBranch());
        assertNotNull(saved.getCheckInTime());
        assertNotNull(employee.getLastCheckIn());
        verify(employeeRepository).save(employee);
    }

    @Test
    void checkInShouldRejectWhenEmployeeHasOpenShift() {
        Employee employee = employee(1L, "Staff A", branch(10L, "Branch A"));

        when(branchAccessService.getEmployee("staff")).thenReturn(employee);
        when(attendanceRecordRepository.findFirstByEmployee_IdAndStatusOrderByCheckInTimeDesc(
                1L, AttendanceStatus.CHECKED_IN)).thenReturn(Optional.of(new AttendanceRecord()));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> attendanceService.checkIn("staff"));

        assertNotNull(ex.getMessage());
        verify(attendanceRecordRepository, never()).save(any());
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void checkInShouldRejectEmployeeWithoutBranch() {
        Employee employee = employee(1L, "Staff A", null);

        when(branchAccessService.getEmployee("staff")).thenReturn(employee);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> attendanceService.checkIn("staff"));

        assertNotNull(ex.getMessage());
        verify(attendanceRecordRepository, never()).save(any());
    }

    @Test
    void checkOutShouldCloseOpenShiftAndCalculateWorkMinutes() {
        Employee employee = employee(1L, "Staff A", branch(10L, "Branch A"));
        AttendanceRecord record = attendanceRecord(employee, LocalDateTime.now().minusHours(2), null,
                AttendanceStatus.CHECKED_IN, null);

        when(branchAccessService.getEmployee("staff")).thenReturn(employee);
        when(attendanceRecordRepository.findFirstByEmployee_IdAndStatusOrderByCheckInTimeDesc(
                1L, AttendanceStatus.CHECKED_IN)).thenReturn(Optional.of(record));
        when(attendanceRecordRepository.save(record)).thenReturn(record);

        AttendanceRecordResponse response = attendanceService.checkOut("staff");

        assertEquals(AttendanceStatus.CHECKED_OUT, record.getStatus());
        assertNotNull(record.getCheckOutTime());
        assertNotNull(record.getUpdatedAt());
        assertTrue(record.getWorkMinutes() >= 119);
        assertEquals(record.getWorkMinutes(), response.getWorkMinutes());
        verify(attendanceRecordRepository).save(record);
    }

    @Test
    void checkOutShouldRejectWhenNoOpenShiftExists() {
        Employee employee = employee(1L, "Staff A", branch(10L, "Branch A"));

        when(branchAccessService.getEmployee("staff")).thenReturn(employee);
        when(attendanceRecordRepository.findFirstByEmployee_IdAndStatusOrderByCheckInTimeDesc(
                1L, AttendanceStatus.CHECKED_IN)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> attendanceService.checkOut("staff"));

        assertNotNull(ex.getMessage());
        verify(attendanceRecordRepository, never()).save(any());
    }

    @Test
    void getMyAttendanceShouldUseRequestedDateRange() {
        Employee employee = employee(1L, "Staff A", branch(10L, "Branch A"));
        AttendanceRecord record = attendanceRecord(employee, LocalDateTime.of(2026, 5, 3, 9, 0),
                LocalDateTime.of(2026, 5, 3, 17, 0), AttendanceStatus.CHECKED_OUT, 480L);

        when(branchAccessService.getEmployee("staff")).thenReturn(employee);
        when(attendanceRecordRepository.findByEmployee_IdAndCheckInTimeBetweenOrderByCheckInTimeDesc(
                eq(1L),
                eq(LocalDateTime.of(2026, 5, 1, 0, 0)),
                eq(LocalDateTime.of(2026, 5, 6, 0, 0))
        )).thenReturn(List.of(record));

        List<AttendanceRecordResponse> responses = attendanceService.getMyAttendance(
                "staff",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 5)
        );

        assertEquals(1, responses.size());
        assertEquals(480L, responses.get(0).getWorkMinutes());
        assertEquals(8.0, responses.get(0).getWorkHours());
    }

    @Test
    void getMyAttendanceShouldRejectInvalidDateRange() {
        when(branchAccessService.getEmployee("staff")).thenReturn(employee(1L, "Staff A", branch(10L, "Branch A")));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> attendanceService.getMyAttendance(
                "staff",
                LocalDate.of(2026, 5, 10),
                LocalDate.of(2026, 5, 1)
        ));

        assertNotNull(ex.getMessage());
    }

    @Test
    void getEmployeeAttendanceShouldAllowSuperAdmin() {
        Employee target = employee(2L, "Staff B", branch(20L, "Branch B"));
        Account admin = account(RoleName.SUPER_ADMIN);

        when(employeeRepository.findById(2L)).thenReturn(Optional.of(target));
        when(branchAccessService.getAccount("admin")).thenReturn(admin);
        when(attendanceRecordRepository.findByEmployee_IdAndCheckInTimeBetweenOrderByCheckInTimeDesc(
                eq(2L), any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(List.of(attendanceRecord(target, LocalDateTime.now(), null, AttendanceStatus.CHECKED_IN, null)));

        List<AttendanceRecordResponse> responses = attendanceService.getEmployeeAttendance(
                2L,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 1),
                "admin"
        );

        assertEquals(1, responses.size());
    }

    @Test
    void getEmployeeAttendanceShouldAskBranchAccessForManager() {
        Branch branch = branch(20L, "Branch B");
        Employee target = employee(2L, "Staff B", branch);
        Account manager = account(RoleName.MANAGER);

        when(employeeRepository.findById(2L)).thenReturn(Optional.of(target));
        when(branchAccessService.getAccount("manager")).thenReturn(manager);

        attendanceService.getEmployeeAttendance(
                2L,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 1),
                "manager"
        );

        verify(branchAccessService).assertCanAccessBranch("manager", branch);
    }

    @Test
    void getEmployeeAttendanceShouldRejectStaffViewingAnotherEmployee() {
        Employee target = employee(2L, "Staff B", branch(20L, "Branch B"));
        Employee requester = employee(3L, "Staff C", branch(20L, "Branch B"));

        when(employeeRepository.findById(2L)).thenReturn(Optional.of(target));
        when(branchAccessService.getAccount("staff")).thenReturn(account(RoleName.STAFF));
        when(branchAccessService.getEmployee("staff")).thenReturn(requester);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> attendanceService.getEmployeeAttendance(
                2L,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 1),
                "staff"
        ));

        assertNotNull(ex.getMessage());
    }

    @Test
    void getClosedRecordsForEmployeeShouldReturnOnlyClosedRowsWithWorkMinutes() {
        Employee employee = employee(1L, "Staff A", branch(10L, "Branch A"));
        AttendanceRecord checkedOut = attendanceRecord(employee, LocalDateTime.now(), null,
                AttendanceStatus.CHECKED_OUT, 480L);
        AttendanceRecord adjusted = attendanceRecord(employee, LocalDateTime.now(), null,
                AttendanceStatus.MANUAL_ADJUSTED, 60L);
        AttendanceRecord open = attendanceRecord(employee, LocalDateTime.now(), null,
                AttendanceStatus.CHECKED_IN, null);
        AttendanceRecord missingMinutes = attendanceRecord(employee, LocalDateTime.now(), null,
                AttendanceStatus.CHECKED_OUT, null);

        when(attendanceRecordRepository.findByEmployee_IdAndCheckInTimeBetweenOrderByCheckInTimeDesc(
                eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(List.of(checkedOut, adjusted, open, missingMinutes));

        List<AttendanceRecord> records = attendanceService.getClosedRecordsForEmployee(
                1L,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31)
        );

        assertEquals(List.of(checkedOut, adjusted), records);
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

    private Employee employee(Long id, String fullName, Branch branch) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setFullName(fullName);
        employee.setBranch(branch);
        return employee;
    }

    private AttendanceRecord attendanceRecord(Employee employee, LocalDateTime checkInTime,
                                              LocalDateTime checkOutTime, AttendanceStatus status,
                                              Long workMinutes) {
        AttendanceRecord record = new AttendanceRecord();
        record.setId(100L);
        record.setEmployee(employee);
        record.setBranch(employee.getBranch());
        record.setCheckInTime(checkInTime);
        record.setCheckOutTime(checkOutTime);
        record.setStatus(status);
        record.setWorkMinutes(workMinutes);
        record.setCreatedAt(checkInTime);
        return record;
    }
}
