package com.pizzastore.service;

import com.pizzastore.dto.AttendanceRecordResponse;
import com.pizzastore.entity.Account;
import com.pizzastore.entity.AttendanceRecord;
import com.pizzastore.entity.Branch;
import com.pizzastore.entity.Employee;
import com.pizzastore.enums.AttendanceStatus;
import com.pizzastore.enums.RoleName;
import com.pizzastore.repository.AttendanceRecordRepository;
import com.pizzastore.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AttendanceService {
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final EmployeeRepository employeeRepository;
    private final BranchAccessService branchAccessService;

    public AttendanceService(AttendanceRecordRepository attendanceRecordRepository,
                             EmployeeRepository employeeRepository,
                             BranchAccessService branchAccessService) {
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.employeeRepository = employeeRepository;
        this.branchAccessService = branchAccessService;
    }

    @Transactional
    public AttendanceRecordResponse checkIn(String username) {
        Employee employee = branchAccessService.getEmployee(username);
        Branch branch = requireBranch(employee);

        attendanceRecordRepository.findFirstByEmployee_IdAndStatusOrderByCheckInTimeDesc(
                        employee.getId(),
                        AttendanceStatus.CHECKED_IN
                )
                .ifPresent(existing -> {
                    throw new RuntimeException("Nhân viên đang có ca chưa check-out.");
                });

        LocalDateTime now = LocalDateTime.now();
        AttendanceRecord record = new AttendanceRecord();
        record.setEmployee(employee);
        record.setBranch(branch);
        record.setCheckInTime(now);
        record.setStatus(AttendanceStatus.CHECKED_IN);
        record.setCreatedAt(now);

        employee.setLastCheckIn(now);
        employeeRepository.save(employee);
        return toResponse(attendanceRecordRepository.save(record));
    }

    @Transactional
    public AttendanceRecordResponse checkOut(String username) {
        Employee employee = branchAccessService.getEmployee(username);
        AttendanceRecord record = attendanceRecordRepository.findFirstByEmployee_IdAndStatusOrderByCheckInTimeDesc(
                        employee.getId(),
                        AttendanceStatus.CHECKED_IN
                )
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ca đang check-in để check-out."));

        LocalDateTime now = LocalDateTime.now();
        long workMinutes = Math.max(0, Duration.between(record.getCheckInTime(), now).toMinutes());
        record.setCheckOutTime(now);
        record.setWorkMinutes(workMinutes);
        record.setStatus(AttendanceStatus.CHECKED_OUT);
        record.setUpdatedAt(now);

        return toResponse(attendanceRecordRepository.save(record));
    }

    public List<AttendanceRecordResponse> getMyAttendance(String username, LocalDate from, LocalDate to) {
        Employee employee = branchAccessService.getEmployee(username);
        DateRange range = toRange(from, to);
        return attendanceRecordRepository
                .findByEmployee_IdAndCheckInTimeBetweenOrderByCheckInTimeDesc(employee.getId(), range.from(), range.to())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<AttendanceRecordResponse> getEmployeeAttendance(Long employeeId, LocalDate from, LocalDate to, String requesterUsername) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên ID: " + employeeId));
        assertCanViewEmployee(requesterUsername, employee);

        DateRange range = toRange(from, to);
        return attendanceRecordRepository
                .findByEmployee_IdAndCheckInTimeBetweenOrderByCheckInTimeDesc(employeeId, range.from(), range.to())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<AttendanceRecord> getClosedRecordsForEmployee(Long employeeId, LocalDate from, LocalDate to) {
        DateRange range = toRange(from, to);
        return attendanceRecordRepository
                .findByEmployee_IdAndCheckInTimeBetweenOrderByCheckInTimeDesc(employeeId, range.from(), range.to())
                .stream()
                .filter(record -> record.getStatus() == AttendanceStatus.CHECKED_OUT
                        || record.getStatus() == AttendanceStatus.MANUAL_ADJUSTED)
                .filter(record -> record.getWorkMinutes() != null)
                .collect(Collectors.toList());
    }

    private void assertCanViewEmployee(String requesterUsername, Employee employee) {
        Account account = branchAccessService.getAccount(requesterUsername);
        if (account.getRole() == RoleName.SUPER_ADMIN) {
            return;
        }
        if (account.getRole() == RoleName.MANAGER) {
            branchAccessService.assertCanAccessBranch(requesterUsername, employee.getBranch());
            return;
        }
        Employee requester = branchAccessService.getEmployee(requesterUsername);
        if (!requester.getId().equals(employee.getId())) {
            throw new RuntimeException("Bạn chỉ được xem chấm công của chính mình.");
        }
    }

    private Branch requireBranch(Employee employee) {
        if (employee.getBranch() == null) {
            throw new RuntimeException("Nhân viên chưa được gán chi nhánh.");
        }
        return employee.getBranch();
    }

    private DateRange toRange(LocalDate from, LocalDate to) {
        LocalDate safeFrom = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate safeTo = to != null ? to : LocalDate.now();
        if (safeTo.isBefore(safeFrom)) {
            throw new RuntimeException("Ngày kết thúc không được nhỏ hơn ngày bắt đầu.");
        }
        return new DateRange(safeFrom.atStartOfDay(), safeTo.plusDays(1).atStartOfDay());
    }

    private AttendanceRecordResponse toResponse(AttendanceRecord record) {
        Long workMinutes = record.getWorkMinutes();
        Double workHours = workMinutes == null ? null : workMinutes / 60.0;
        return new AttendanceRecordResponse(
                record.getId(),
                record.getEmployee().getId(),
                record.getEmployee().getFullName(),
                record.getBranch().getId(),
                record.getBranch().getName(),
                record.getCheckInTime(),
                record.getCheckOutTime(),
                workMinutes,
                workHours,
                record.getStatus(),
                record.getNote()
        );
    }

    private record DateRange(LocalDateTime from, LocalDateTime to) {
    }
}
