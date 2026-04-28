package com.pizzastore.controller;

import com.pizzastore.entity.Coupon;
import com.pizzastore.repository.CouponRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@Tag(name = "4. Mã Giảm Giá (Coupon)", description = "Quản lý các chương trình khuyến mãi, Voucher")
public class CouponController {

    private final CouponRepository couponRepository;

    @Autowired
    public CouponController(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    @Operation(summary = "Tạo mã giảm giá mới", description = "Chỉ Manager mới có quyền tạo. Hỗ trợ giảm theo % hoặc số tiền cố định.")
    public ResponseEntity<?> createCoupon(@RequestBody Coupon coupon) {
        try {
            coupon.setCode(coupon.getCode().toUpperCase());
            if (couponRepository.existsByCode(coupon.getCode())) {
                return ResponseEntity.badRequest().body("Mã giảm giá này đã tồn tại!");
            }

            if (coupon.getExpirationDate() != null
                    && coupon.getExpirationDate().isBefore(LocalDate.now())) {
                return ResponseEntity.badRequest().body("Ngày hết hạn phải lớn hơn hoặc bằng hôm nay!");
            }

            boolean hasPercent = coupon.getDiscountPercent() != null && coupon.getDiscountPercent() > 0;
            boolean hasAmount = coupon.getDiscountAmount() != null && coupon.getDiscountAmount() > 0;

            if (!hasPercent && !hasAmount) {
                return ResponseEntity.badRequest().body("Vui lòng nhập phần trăm giảm giá HOẶC số tiền giảm giá!");
            }
            if (hasPercent && hasAmount) {
                return ResponseEntity.badRequest().body("Chỉ được chọn 1 trong 2: Giảm theo % hoặc Giảm tiền mặt!");
            }
            if (hasPercent && coupon.getDiscountPercent() > 100) {
                return ResponseEntity.badRequest().body("Phần trăm giảm giá không thể quá 100%!");
            }

            coupon.setUsageCount(0);
            if (!coupon.isActive()) coupon.setActive(true);

            Coupon savedCoupon = couponRepository.save(coupon);
            return ResponseEntity.ok(savedCoupon);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi tạo mã: " + e.getMessage());
        }
    }

    @GetMapping("/manager")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    @Operation(summary = "Xem tất cả mã giảm giá (Nội bộ)", description = "Hiển thị toàn bộ mã, bao gồm cả mã đã hết hạn hoặc bị vô hiệu hóa.")
    public ResponseEntity<?> getAllCouponsForManager() {
        return ResponseEntity.ok(couponRepository.findAll());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    @Operation(summary = "Xóa mã giảm giá", description = "Xóa vĩnh viễn một mã giảm giá khỏi cơ sở dữ liệu.")
    public ResponseEntity<?> deleteCoupon(@PathVariable Long id) {
        if (!couponRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        couponRepository.deleteById(id);
        return ResponseEntity.ok("Đã xóa mã giảm giá thành công");
    }

    @PutMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'MANAGER')")
    @Operation(summary = "Kích hoạt / Vô hiệu hóa mã", description = "Thay đổi trạng thái Active của mã (bật/tắt) để cho phép hoặc ngưng sử dụng.")
    public ResponseEntity<?> toggleCoupon(@PathVariable Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mã"));

        coupon.setActive(!coupon.isActive());
        return ResponseEntity.ok(couponRepository.save(coupon));
    }

    @GetMapping("/public")
    @Operation(summary = "Xem mã giảm giá đang chạy (Public)", description = "Dành cho khách hàng xem các khuyến mãi đang diễn ra và còn hiệu lực.")
    public ResponseEntity<?> getAvailableCoupons() {
        List<Coupon> validCoupons = couponRepository.findValidCoupons();
        return ResponseEntity.ok(validCoupons);
    }

    @PostMapping("/check")
    @Operation(summary = "Kiểm tra mã giảm giá", description = "Khách hàng nhập mã để check xem có hợp lệ trước khi đặt món hay không.")
    public ResponseEntity<?> checkCouponCode(@RequestBody String code) {
        try {
            Coupon coupon = couponRepository.findByCode(code.toUpperCase())
                    .orElseThrow(() -> new RuntimeException("Mã không tồn tại"));

            if (!coupon.isActive() || (coupon.getExpirationDate() != null && coupon.getExpirationDate().isBefore(LocalDate.now()))) {
                return ResponseEntity.badRequest().body("Mã không hợp lệ hoặc đã hết hạn");
            }
            return ResponseEntity.ok(coupon);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
