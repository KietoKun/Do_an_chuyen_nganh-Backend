package com.pizzastore.controller;

import com.pizzastore.entity.Coupon;
import com.pizzastore.repository.CouponRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    private final CouponRepository couponRepository;

    @Autowired
    public CouponController(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    // 1. Tạo mã giảm giá mới
    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
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

            // Nếu giảm theo % thì NÊN có Max Cap (cảnh báo hoặc bắt buộc tùy bạn)
            if (hasPercent && (coupon.getMaxDiscountAmount() == null || coupon.getMaxDiscountAmount() <= 0)) {
                // return ResponseEntity.badRequest().body("Vui lòng nhập số tiền giảm tối đa (Max Cap) khi giảm theo %!");
            }
            // ----------------------------------------------

            coupon.setUsageCount(0);
            if (!coupon.isActive()) coupon.setActive(true);

            Coupon savedCoupon = couponRepository.save(coupon);
            return ResponseEntity.ok(savedCoupon);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi tạo mã: " + e.getMessage());
        }
    }

    // 2. Quản lý: Lấy tất cả mã (bao gồm cả mã ẩn/hết hạn)
    @GetMapping("/manager")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> getAllCouponsForManager() {
        return ResponseEntity.ok(couponRepository.findAll());
    }

    // 3. Xóa hoặc Khóa mã
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> deleteCoupon(@PathVariable Long id) {
        if (!couponRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        couponRepository.deleteById(id);
        return ResponseEntity.ok("Đã xóa mã giảm giá thành công");
    }

    // API phụ: Kích hoạt/Vô hiệu hóa mã (Soft Delete)
    @PutMapping("/{id}/toggle")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<?> toggleCoupon(@PathVariable Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mã"));

        coupon.setActive(!coupon.isActive()); // Đảo ngược trạng thái
        return ResponseEntity.ok(couponRepository.save(coupon));
    }

    // 4. Lấy danh sách mã đang khuyến mãi (Ai cũng xem được)
    @GetMapping("/public")
    public ResponseEntity<?> getAvailableCoupons() {
        List<Coupon> validCoupons = couponRepository.findValidCoupons();
        return ResponseEntity.ok(validCoupons);
    }

    // 5. API Kiểm tra mã nhanh
    @PostMapping("/check")
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