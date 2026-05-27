package com.pizzastore.controller.integration;

import com.pizzastore.config.WebSecurityConfig;
import com.pizzastore.controller.PayrollController;
import com.pizzastore.dto.PayrollRecordResponse;
import com.pizzastore.dto.PayrollSummaryResponse;
import com.pizzastore.enums.PayrollStatus;
import com.pizzastore.security.JwtUtils;
import com.pizzastore.security.UserDetailsServiceImpl;
import com.pizzastore.service.PayrollService;
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

@WebMvcTest(controllers = PayrollController.class)
@Import(WebSecurityConfig.class)
@ActiveProfiles("test")
class PayrollControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PayrollService payrollService;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private JwtUtils jwtUtils;

    @Test
    @WithMockUser(username = "staff", roles = "STAFF")
    void previewPayrollShouldAllowEmployeeRolesAndPassUsername() throws Exception {
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 31);
        when(payrollService.calculateEmployeePayroll(10L, from, to, "staff"))
                .thenReturn(summary());

        mockMvc.perform(get("/api/payroll/employees/10/preview")
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value(10))
                .andExpect(jsonPath("$.totalWorkMinutes").value(480));

        verify(payrollService).calculateEmployeePayroll(10L, from, to, "staff");
    }

    @Test
    @WithMockUser(username = "staff", roles = "STAFF")
    void generatePayrollShouldRejectStaffRole() throws Exception {
        mockMvc.perform(post("/api/payroll/generate")
                        .param("branchId", "1")
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-31"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void generatePayrollShouldPassBranchAndDatesToService() throws Exception {
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 31);
        when(payrollService.generatePayroll(1L, from, to, "manager")).thenReturn(List.of(record(PayrollStatus.DRAFT)));

        mockMvc.perform(post("/api/payroll/generate")
                        .param("branchId", "1")
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("DRAFT"));

        verify(payrollService).generatePayroll(1L, from, to, "manager");
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void getPayrollRecordsShouldReturnBadRequestWhenServiceRejects() throws Exception {
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 31);
        when(payrollService.getPayrollRecords(2L, from, to, "manager"))
                .thenThrow(new RuntimeException("Forbidden branch"));

        mockMvc.perform(get("/api/payroll")
                        .param("branchId", "2")
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-31"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void confirmPayrollShouldCallService() throws Exception {
        when(payrollService.confirmPayroll(99L, "manager")).thenReturn(record(PayrollStatus.CONFIRMED));

        mockMvc.perform(post("/api/payroll/99/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        verify(payrollService).confirmPayroll(99L, "manager");
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void markPaidShouldCallService() throws Exception {
        when(payrollService.markPaid(99L, "manager")).thenReturn(record(PayrollStatus.PAID));

        mockMvc.perform(post("/api/payroll/99/mark-paid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        verify(payrollService).markPaid(99L, "manager");
    }

    private PayrollSummaryResponse summary() {
        return new PayrollSummaryResponse(
                10L,
                "Staff A",
                1L,
                "Branch A",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                50000.0,
                480L,
                8.0,
                400000.0,
                0.0,
                0.0,
                400000.0
        );
    }

    private PayrollRecordResponse record(PayrollStatus status) {
        return new PayrollRecordResponse(
                99L,
                10L,
                "Staff A",
                1L,
                "Branch A",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                50000.0,
                480L,
                8.0,
                400000.0,
                0.0,
                0.0,
                400000.0,
                status,
                LocalDateTime.of(2026, 5, 31, 12, 0),
                status == PayrollStatus.CONFIRMED ? LocalDateTime.of(2026, 5, 31, 13, 0) : null,
                status == PayrollStatus.PAID ? LocalDateTime.of(2026, 5, 31, 14, 0) : null
        );
    }
}
