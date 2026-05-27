package com.pizzastore.controller.integration;

import com.pizzastore.config.WebSecurityConfig;
import com.pizzastore.controller.AttendanceController;
import com.pizzastore.dto.AttendanceRecordResponse;
import com.pizzastore.enums.AttendanceStatus;
import com.pizzastore.security.JwtUtils;
import com.pizzastore.security.UserDetailsServiceImpl;
import com.pizzastore.service.AttendanceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AttendanceController.class)
@Import(WebSecurityConfig.class)
@ActiveProfiles("test")
class AttendanceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AttendanceService attendanceService;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private JwtUtils jwtUtils;

    @Test
    @WithMockUser(username = "staff", roles = "STAFF")
    void checkInShouldUseAuthenticatedUsername() throws Exception {
        when(attendanceService.checkIn("staff")).thenReturn(response(1L, AttendanceStatus.CHECKED_IN, null));

        mockMvc.perform(post("/api/attendance/check-in"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("CHECKED_IN"));

        verify(attendanceService).checkIn("staff");
    }

    @Test
    @WithMockUser(username = "staff", roles = "STAFF")
    void checkOutShouldReturnBadRequestWhenServiceRejects() throws Exception {
        when(attendanceService.checkOut("staff")).thenThrow(new RuntimeException("No open shift"));

        mockMvc.perform(post("/api/attendance/check-out"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "staff", roles = "STAFF")
    void getMyAttendanceShouldPassDateRangeToService() throws Exception {
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 31);
        when(attendanceService.getMyAttendance("staff", from, to))
                .thenReturn(List.of(response(1L, AttendanceStatus.CHECKED_OUT, 480L)));

        mockMvc.perform(get("/api/attendance/me")
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].workMinutes").value(480));

        verify(attendanceService).getMyAttendance("staff", from, to);
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void managerShouldGetEmployeeAttendance() throws Exception {
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 31);
        when(attendanceService.getEmployeeAttendance(10L, from, to, "manager"))
                .thenReturn(List.of(response(2L, AttendanceStatus.CHECKED_OUT, 480L)));

        mockMvc.perform(get("/api/attendance/employees/10")
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2));

        verify(attendanceService).getEmployeeAttendance(10L, from, to, "manager");
    }

    @Test
    @WithMockUser(username = "staff", roles = "STAFF")
    void employeeAttendanceEndpointShouldRejectStaffRole() throws Exception {
        mockMvc.perform(get("/api/attendance/employees/10")
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-31"))
                .andExpect(status().isForbidden());
    }

    private AttendanceRecordResponse response(Long id, AttendanceStatus status, Long workMinutes) {
        return new AttendanceRecordResponse(
                id,
                10L,
                "Staff A",
                1L,
                "Branch A",
                LocalDateTime.of(2026, 5, 1, 9, 0),
                status == AttendanceStatus.CHECKED_OUT ? LocalDateTime.of(2026, 5, 1, 17, 0) : null,
                workMinutes,
                workMinutes == null ? null : workMinutes / 60.0,
                status,
                null
        );
    }
}
