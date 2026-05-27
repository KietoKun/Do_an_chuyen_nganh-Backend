package com.pizzastore.controller.integration;

import com.pizzastore.config.WebSecurityConfig;
import com.pizzastore.controller.CouponController;
import com.pizzastore.entity.Coupon;
import com.pizzastore.repository.CouponRepository;
import com.pizzastore.security.JwtUtils;
import com.pizzastore.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CouponController.class)
@Import(WebSecurityConfig.class)
@ActiveProfiles("test")
class CouponControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CouponRepository couponRepository;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private JwtUtils jwtUtils;

    @Test
    @WithMockUser(roles = "MANAGER")
    void createCouponShouldUppercaseCodeAndReturnSavedCoupon() throws Exception {
        when(couponRepository.existsByCode("SAVE10")).thenReturn(false);
        when(couponRepository.save(any(Coupon.class))).thenAnswer(invocation -> {
            Coupon coupon = invocation.getArgument(0);
            coupon.setId(1L);
            return coupon;
        });

        mockMvc.perform(post("/api/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "save10",
                                  "discountPercent": 10,
                                  "expirationDate": "2026-12-31"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("SAVE10"))
                .andExpect(jsonPath("$.usageCount").value(0))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void createCouponShouldRejectDuplicateCode() throws Exception {
        when(couponRepository.existsByCode("SAVE10")).thenReturn(true);

        mockMvc.perform(post("/api/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "save10",
                                  "discountPercent": 10,
                                  "expirationDate": "2026-12-31"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(couponRepository, never()).save(any());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void createCouponShouldRejectBothPercentAndAmount() throws Exception {
        when(couponRepository.existsByCode("SAVE10")).thenReturn(false);

        mockMvc.perform(post("/api/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "save10",
                                  "discountPercent": 10,
                                  "discountAmount": 10000,
                                  "expirationDate": "2026-12-31"
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(couponRepository, never()).save(any());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void managerEndpointShouldRejectCustomerRole() throws Exception {
        mockMvc.perform(get("/api/coupons/manager"))
                .andExpect(status().isForbidden());
    }

    @Test
    void publicCouponsShouldBeAccessibleWithoutAuthentication() throws Exception {
        when(couponRepository.findValidCoupons()).thenReturn(List.of(coupon(1L, "PUBLIC10", true)));

        mockMvc.perform(get("/api/coupons/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("PUBLIC10"));
    }

    @Test
    void checkCouponShouldReturnCouponWhenActiveAndNotExpired() throws Exception {
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(coupon(1L, "SAVE10", true)));

        mockMvc.perform(post("/api/coupons/check")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("save10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SAVE10"));
    }

    @Test
    void checkCouponShouldRejectInactiveCoupon() throws Exception {
        when(couponRepository.findByCode("SAVE10")).thenReturn(Optional.of(coupon(1L, "SAVE10", false)));

        mockMvc.perform(post("/api/coupons/check")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("save10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void toggleCouponShouldFlipActiveFlag() throws Exception {
        Coupon coupon = coupon(1L, "SAVE10", true);
        when(couponRepository.findById(1L)).thenReturn(Optional.of(coupon));
        when(couponRepository.save(coupon)).thenReturn(coupon);

        mockMvc.perform(put("/api/coupons/1/toggle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void deleteCouponShouldReturnNotFoundWhenMissing() throws Exception {
        when(couponRepository.existsById(1L)).thenReturn(false);

        mockMvc.perform(delete("/api/coupons/1"))
                .andExpect(status().isNotFound());
    }

    private Coupon coupon(Long id, String code, boolean active) {
        Coupon coupon = new Coupon();
        coupon.setId(id);
        coupon.setCode(code);
        coupon.setDiscountPercent(10.0);
        coupon.setExpirationDate(LocalDate.now().plusDays(7));
        coupon.setUsageCount(0);
        coupon.setActive(active);
        return coupon;
    }
}
