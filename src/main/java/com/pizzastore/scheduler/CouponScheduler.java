package com.pizzastore.scheduler;

import com.pizzastore.repository.CouponRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CouponScheduler {

    private static final Logger logger = LoggerFactory.getLogger(CouponScheduler.class);
    private final CouponRepository couponRepository;

    @Autowired
    public CouponScheduler(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    // Cron: Chạy vào đúng 00:00:00 (nửa đêm) mỗi ngày
    @Scheduled(cron = "0 0 0 * * ?")
    public void scanAndDeactivateExpiredCoupons() {
        logger.info("Bắt đầu quét các mã giảm giá hết hạn...");
        int updatedCount = couponRepository.deactivateExpiredCoupons();
        logger.info("Đã cập nhật và vô hiệu hóa thành công {} mã giảm giá.", updatedCount);
    }
}