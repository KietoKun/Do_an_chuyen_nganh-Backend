package com.pizzastore.controller;

import com.pizzastore.dto.AttendanceRecordResponse;
import com.pizzastore.service.AttendanceService;
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
@RequestMapping("/api/attendance")
@Tag(name = "14. Chấm công", description = "API chấm công theo từng ca làm, lưu lịch sử check-in/check-out để tính lương.")
public class AttendanceController {
    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping("/check-in")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER', 'STAFF', 'CHEF')")
    @Operation(
            summary = "Nhân viên check-in bắt đầu ca làm",
            description = """
                    Luồng hoạt động:
                    1. Nhân viên đăng nhập và gọi API này khi bắt đầu ca.
                    2. Backend lấy username từ JWT, tìm hồ sơ nhân viên và chi nhánh đang được gán.
                    3. Nếu nhân viên đang có một ca CHECKED_IN chưa check-out, hệ thống từ chối để tránh trùng ca.
                    4. Nếu hợp lệ, hệ thống tạo một bản ghi chấm công với checkInTime là thời điểm hiện tại và status = CHECKED_IN.

                    Lưu ý:
                    - Request body không cần truyền employeeId vì backend xác định nhân viên từ token đăng nhập.
                    - Bản ghi này chưa được tính vào lương cho đến khi nhân viên check-out.
                    """
    )
    public ResponseEntity<?> checkIn() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            return ResponseEntity.ok(attendanceService.checkIn(username));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/check-out")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER', 'STAFF', 'CHEF')")
    @Operation(
            summary = "Nhân viên check-out kết thúc ca làm",
            description = """
                    Luồng hoạt động:
                    1. Nhân viên gọi API này khi kết thúc ca.
                    2. Backend tìm ca CHECKED_IN gần nhất của nhân viên.
                    3. Hệ thống cập nhật checkOutTime là thời điểm hiện tại.
                    4. Hệ thống tính workMinutes = số phút từ checkInTime đến checkOutTime.
                    5. Status chuyển sang CHECKED_OUT.

                    Quy tắc tính công:
                    - Chỉ các bản ghi đã CHECKED_OUT hoặc MANUAL_ADJUSTED và có workMinutes mới được dùng để tính lương.
                    - Ca chưa check-out không được cộng vào tổng giờ công để tránh tính sai lương.
                    """
    )
    public ResponseEntity<?> checkOut() {
        try {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            return ResponseEntity.ok(attendanceService.checkOut(username));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER', 'STAFF', 'CHEF')")
    @Operation(
            summary = "Xem lịch sử chấm công của tài khoản đang đăng nhập",
            description = """
                    Trả về danh sách bản ghi chấm công của chính nhân viên đang đăng nhập.
                    Nếu không truyền from/to, hệ thống mặc định lấy từ đầu tháng hiện tại đến hôm nay.
                    """
    )
    public ResponseEntity<List<AttendanceRecordResponse>> getMyAttendance(
            @Parameter(description = "Ngày bắt đầu lọc, định dạng yyyy-MM-dd", example = "2026-05-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Ngày kết thúc lọc, định dạng yyyy-MM-dd", example = "2026-05-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(attendanceService.getMyAttendance(username, from, to));
    }

    @GetMapping("/employees/{employeeId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    @Operation(
            summary = "Quản lý xem lịch sử chấm công của một nhân viên",
            description = """
                    SUPER_ADMIN được xem mọi nhân viên.
                    MANAGER chỉ được xem nhân viên thuộc chi nhánh của mình.

                    Dữ liệu này là nguồn đầu vào cho API tính lương:
                    tổng phút công = tổng workMinutes của các ca đã CHECKED_OUT hoặc MANUAL_ADJUSTED trong khoảng ngày lọc.
                    """
    )
    public ResponseEntity<List<AttendanceRecordResponse>> getEmployeeAttendance(
            @PathVariable Long employeeId,
            @Parameter(description = "Ngày bắt đầu lọc, định dạng yyyy-MM-dd", example = "2026-05-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Ngày kết thúc lọc, định dạng yyyy-MM-dd", example = "2026-05-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(attendanceService.getEmployeeAttendance(employeeId, from, to, username));
    }
}
