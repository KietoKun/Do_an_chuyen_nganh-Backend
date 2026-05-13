package com.pizzastore.controller;

import com.pizzastore.dto.PayrollRecordResponse;
import com.pizzastore.dto.PayrollSummaryResponse;
import com.pizzastore.service.PayrollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/payroll")
@Tag(name = "15. Tính lương", description = "API tính lương từ dữ liệu chấm công và đơn giá lương theo giờ của nhân viên.")
public class PayrollController {
    private final PayrollService payrollService;

    public PayrollController(PayrollService payrollService) {
        this.payrollService = payrollService;
    }

    @GetMapping("/employees/{employeeId}/preview")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER', 'STAFF', 'CHEF')")
    @Operation(
            summary = "Xem trước lương của một nhân viên theo kỳ",
            description = """
                    API này chỉ tính thử và không lưu bảng lương.

                    Công thức tính:
                    1. Lấy tất cả bản ghi chấm công của nhân viên trong khoảng from/to.
                    2. Chỉ cộng các ca có status = CHECKED_OUT hoặc MANUAL_ADJUSTED và có workMinutes.
                    3. totalWorkMinutes = tổng workMinutes của các ca hợp lệ.
                    4. totalWorkHours = totalWorkMinutes / 60.
                    5. grossSalary = totalWorkHours * employee.salaryPerHour.
                    6. netSalary = grossSalary + bonus - deduction.

                    Ở phiên bản preview hiện tại, bonus = 0 và deduction = 0.

                    Quyền truy cập:
                    - STAFF/CHEF chỉ xem được lương của chính mình.
                    - MANAGER xem được nhân viên thuộc chi nhánh của mình.
                    - SUPER_ADMIN xem được toàn hệ thống.
                    """
    )
    public ResponseEntity<?> previewEmployeePayroll(
            @PathVariable Long employeeId,
            @Parameter(description = "Ngày bắt đầu kỳ lương, định dạng yyyy-MM-dd", example = "2026-05-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Ngày kết thúc kỳ lương, định dạng yyyy-MM-dd", example = "2026-05-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            PayrollSummaryResponse response = payrollService.calculateEmployeePayroll(employeeId, from, to, username);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    @Operation(
            summary = "Tạo bảng lương nháp cho một kỳ",
            description = """
                    API này tạo hoặc cập nhật bảng lương DRAFT từ dữ liệu chấm công.

                    Cách hoạt động:
                    1. Backend xác định danh sách nhân viên theo branchId.
                    2. MANAGER chỉ được tạo bảng lương cho chi nhánh của mình.
                    3. SUPER_ADMIN có thể truyền branchId để tạo cho một chi nhánh, hoặc bỏ trống branchId để tạo toàn hệ thống.
                    4. Với từng nhân viên, hệ thống tính totalWorkMinutes từ các ca đã CHECKED_OUT/MANUAL_ADJUSTED.
                    5. Hệ thống chụp lại salaryPerHour hiện tại của nhân viên vào PayrollRecord.
                    6. grossSalary = totalWorkMinutes / 60 * salaryPerHour.
                    7. netSalary = grossSalary + bonus - deduction. Hiện tại bonus và deduction mặc định là 0.

                    Lưu ý:
                    - Bảng lương ở trạng thái DRAFT vẫn có thể được tạo lại để cập nhật theo dữ liệu chấm công mới.
                    - Nếu bảng lương đã PAID, hệ thống giữ nguyên và không tính lại.
                    """
    )
    public ResponseEntity<?> generatePayroll(
            @Parameter(description = "ID chi nhánh. MANAGER có thể bỏ trống vì backend tự dùng chi nhánh của manager.", example = "1")
            @RequestParam(required = false) Long branchId,
            @Parameter(description = "Ngày bắt đầu kỳ lương, định dạng yyyy-MM-dd", example = "2026-05-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Ngày kết thúc kỳ lương, định dạng yyyy-MM-dd", example = "2026-05-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            List<PayrollRecordResponse> response = payrollService.generatePayroll(branchId, from, to, username);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    @Operation(
            summary = "Xem danh sách bảng lương đã tạo",
            description = """
                    Trả về các PayrollRecord đã được tạo cho đúng kỳ from/to.
                    MANAGER chỉ xem được chi nhánh của mình.
                    SUPER_ADMIN có thể lọc theo branchId hoặc xem toàn hệ thống nếu không truyền branchId.
                    """
    )
    public ResponseEntity<?> getPayrollRecords(
            @RequestParam(required = false) Long branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            return ResponseEntity.ok(payrollService.getPayrollRecords(branchId, from, to, username));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    @Operation(
            summary = "Xác nhận bảng lương",
            description = """
                    Chuyển bảng lương từ DRAFT sang CONFIRMED.
                    Sau khi CONFIRMED, bảng lương được xem là đã chốt nghiệp vụ và có thể đánh dấu đã thanh toán.
                    """
    )
    public ResponseEntity<?> confirmPayroll(@PathVariable Long id) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            return ResponseEntity.ok(payrollService.confirmPayroll(id, username));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/mark-paid")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    @Operation(
            summary = "Đánh dấu bảng lương đã thanh toán",
            description = """
                    Chỉ bảng lương đang ở trạng thái CONFIRMED mới được chuyển sang PAID.
                    Bảng lương đã PAID sẽ không bị tính lại khi gọi generate cho cùng kỳ.
                    """
    )
    public ResponseEntity<?> markPaid(@PathVariable Long id) {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            return ResponseEntity.ok(payrollService.markPaid(id, username));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
