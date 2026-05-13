package com.pizzastore.service;

import com.pizzastore.dto.PayrollRecordResponse;
import com.pizzastore.dto.PayrollSummaryResponse;
import com.pizzastore.entity.Account;
import com.pizzastore.entity.AttendanceRecord;
import com.pizzastore.entity.Branch;
import com.pizzastore.entity.Employee;
import com.pizzastore.entity.PayrollRecord;
import com.pizzastore.enums.PayrollStatus;
import com.pizzastore.enums.RoleName;
import com.pizzastore.repository.BranchRepository;
import com.pizzastore.repository.EmployeeRepository;
import com.pizzastore.repository.PayrollRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PayrollService {
    private final PayrollRecordRepository payrollRecordRepository;
    private final EmployeeRepository employeeRepository;
    private final BranchRepository branchRepository;
    private final AttendanceService attendanceService;
    private final BranchAccessService branchAccessService;

    public PayrollService(PayrollRecordRepository payrollRecordRepository,
                          EmployeeRepository employeeRepository,
                          BranchRepository branchRepository,
                          AttendanceService attendanceService,
                          BranchAccessService branchAccessService) {
        this.payrollRecordRepository = payrollRecordRepository;
        this.employeeRepository = employeeRepository;
        this.branchRepository = branchRepository;
        this.attendanceService = attendanceService;
        this.branchAccessService = branchAccessService;
    }

    public PayrollSummaryResponse calculateEmployeePayroll(Long employeeId, LocalDate from, LocalDate to, String requesterUsername) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên ID: " + employeeId));
        assertCanAccessEmployeePayroll(requesterUsername, employee);
        return calculate(employee, requireFrom(from), requireTo(to), 0.0, 0.0);
    }

    @Transactional
    public List<PayrollRecordResponse> generatePayroll(Long branchId, LocalDate from, LocalDate to, String requesterUsername) {
        LocalDate periodStart = requireFrom(from);
        LocalDate periodEnd = requireTo(to);
        if (periodEnd.isBefore(periodStart)) {
            throw new RuntimeException("Ngày kết thúc không được nhỏ hơn ngày bắt đầu.");
        }

        Long visibleBranchId = branchAccessService.resolveVisibleBranchId(requesterUsername, branchId);
        List<Employee> employees = visibleBranchId == null
                ? employeeRepository.findAll()
                : employeeRepository.findByBranch_Id(visibleBranchId);

        return employees.stream()
                .filter(employee -> employee.getAccount() != null)
                .filter(employee -> employee.getAccount().getRole() == RoleName.MANAGER
                        || employee.getAccount().getRole() == RoleName.STAFF
                        || employee.getAccount().getRole() == RoleName.CHEF)
                .filter(employee -> employee.getBranch() != null)
                .map(employee -> createOrUpdateDraft(employee, periodStart, periodEnd))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<PayrollRecordResponse> getPayrollRecords(Long branchId, LocalDate from, LocalDate to, String requesterUsername) {
        LocalDate periodStart = requireFrom(from);
        LocalDate periodEnd = requireTo(to);
        Long visibleBranchId = branchAccessService.resolveVisibleBranchId(requesterUsername, branchId);

        List<PayrollRecord> records = visibleBranchId == null
                ? payrollRecordRepository.findByPeriodStartAndPeriodEnd(periodStart, periodEnd)
                : payrollRecordRepository.findByBranch_IdAndPeriodStartAndPeriodEnd(
                        visibleBranchId,
                        periodStart,
                        periodEnd
                );
        return records.stream()
                .sorted(Comparator
                        .comparing((PayrollRecord record) -> record.getBranch().getName())
                        .thenComparing(record -> record.getEmployee().getFullName()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PayrollRecordResponse confirmPayroll(Long id, String requesterUsername) {
        PayrollRecord record = getManagedRecord(id, requesterUsername);
        if (record.getStatus() == PayrollStatus.PAID) {
            throw new RuntimeException("Bảng lương đã thanh toán, không thể xác nhận lại.");
        }
        record.setStatus(PayrollStatus.CONFIRMED);
        record.setConfirmedAt(LocalDateTime.now());
        return toResponse(payrollRecordRepository.save(record));
    }

    @Transactional
    public PayrollRecordResponse markPaid(Long id, String requesterUsername) {
        PayrollRecord record = getManagedRecord(id, requesterUsername);
        if (record.getStatus() != PayrollStatus.CONFIRMED) {
            throw new RuntimeException("Chỉ bảng lương đã CONFIRMED mới được đánh dấu PAID.");
        }
        record.setStatus(PayrollStatus.PAID);
        record.setPaidAt(LocalDateTime.now());
        return toResponse(payrollRecordRepository.save(record));
    }

    private PayrollRecord createOrUpdateDraft(Employee employee, LocalDate periodStart, LocalDate periodEnd) {
        PayrollSummaryResponse summary = calculate(employee, periodStart, periodEnd, 0.0, 0.0);
        PayrollRecord record = payrollRecordRepository
                .findByEmployee_IdAndPeriodStartAndPeriodEnd(employee.getId(), periodStart, periodEnd)
                .orElseGet(PayrollRecord::new);

        if (record.getStatus() == PayrollStatus.PAID) {
            return record;
        }

        record.setEmployee(employee);
        record.setBranch(requireBranch(employee));
        record.setPeriodStart(periodStart);
        record.setPeriodEnd(periodEnd);
        record.setTotalWorkMinutes(summary.getTotalWorkMinutes());
        record.setSalaryPerHour(summary.getSalaryPerHour());
        record.setGrossSalary(summary.getGrossSalary());
        record.setBonus(summary.getBonus());
        record.setDeduction(summary.getDeduction());
        record.setNetSalary(summary.getNetSalary());
        record.setStatus(PayrollStatus.DRAFT);
        record.setCreatedAt(record.getCreatedAt() == null ? LocalDateTime.now() : record.getCreatedAt());
        return payrollRecordRepository.save(record);
    }

    private PayrollSummaryResponse calculate(Employee employee, LocalDate periodStart, LocalDate periodEnd,
                                             Double bonus, Double deduction) {
        if (periodEnd.isBefore(periodStart)) {
            throw new RuntimeException("Ngày kết thúc không được nhỏ hơn ngày bắt đầu.");
        }
        Branch branch = requireBranch(employee);
        Double salaryPerHour = employee.getSalaryPerHour() == null ? 0.0 : employee.getSalaryPerHour();
        long totalWorkMinutes = attendanceService.getClosedRecordsForEmployee(employee.getId(), periodStart, periodEnd)
                .stream()
                .map(AttendanceRecord::getWorkMinutes)
                .reduce(0L, Long::sum);
        double totalWorkHours = totalWorkMinutes / 60.0;
        double grossSalary = totalWorkHours * salaryPerHour;
        double safeBonus = bonus == null ? 0.0 : bonus;
        double safeDeduction = deduction == null ? 0.0 : deduction;
        double netSalary = grossSalary + safeBonus - safeDeduction;

        return new PayrollSummaryResponse(
                employee.getId(),
                employee.getFullName(),
                branch.getId(),
                branch.getName(),
                periodStart,
                periodEnd,
                salaryPerHour,
                totalWorkMinutes,
                totalWorkHours,
                grossSalary,
                safeBonus,
                safeDeduction,
                netSalary
        );
    }

    private PayrollRecord getManagedRecord(Long id, String requesterUsername) {
        PayrollRecord record = payrollRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bảng lương ID: " + id));
        Account account = branchAccessService.getAccount(requesterUsername);
        if (account.getRole() == RoleName.SUPER_ADMIN) {
            return record;
        }
        branchAccessService.assertCanAccessBranch(requesterUsername, record.getBranch());
        return record;
    }

    private void assertCanAccessEmployeePayroll(String requesterUsername, Employee employee) {
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
            throw new RuntimeException("Bạn chỉ được xem lương của chính mình.");
        }
    }

    private Branch requireBranch(Employee employee) {
        if (employee.getBranch() == null || employee.getBranch().getId() == null) {
            throw new RuntimeException("Nhân viên chưa được gán chi nhánh.");
        }
        return branchRepository.findById(employee.getBranch().getId())
                .orElse(employee.getBranch());
    }

    private LocalDate requireFrom(LocalDate from) {
        if (from == null) {
            throw new RuntimeException("Vui lòng truyền ngày bắt đầu kỳ lương.");
        }
        return from;
    }

    private LocalDate requireTo(LocalDate to) {
        if (to == null) {
            throw new RuntimeException("Vui lòng truyền ngày kết thúc kỳ lương.");
        }
        return to;
    }

    private PayrollRecordResponse toResponse(PayrollRecord record) {
        double totalWorkHours = record.getTotalWorkMinutes() / 60.0;
        return new PayrollRecordResponse(
                record.getId(),
                record.getEmployee().getId(),
                record.getEmployee().getFullName(),
                record.getBranch().getId(),
                record.getBranch().getName(),
                record.getPeriodStart(),
                record.getPeriodEnd(),
                record.getSalaryPerHour(),
                record.getTotalWorkMinutes(),
                totalWorkHours,
                record.getGrossSalary(),
                record.getBonus(),
                record.getDeduction(),
                record.getNetSalary(),
                record.getStatus(),
                record.getCreatedAt(),
                record.getConfirmedAt(),
                record.getPaidAt()
        );
    }
}
