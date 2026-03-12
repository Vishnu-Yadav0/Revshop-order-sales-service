package com.revshop.salesservice.service;

import com.revshop.salesservice.model.Coupon;
import com.revshop.salesservice.repository.CouponRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class CouponService {

    private final CouponRepository couponRepository;

    public CouponService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    public BigDecimal validateAndCalculateDiscount(String code, BigDecimal orderAmount) {
        Optional<Coupon> couponOpt = couponRepository.findByCode(code.toUpperCase().trim());

        if (couponOpt.isEmpty()) {
            throw new RuntimeException("Coupon not found");
        }

        Coupon coupon = couponOpt.get();

        if (!coupon.getIsActive()) {
            throw new RuntimeException("Coupon is inactive");
        }

        if (coupon.getExpiryDate() != null && coupon.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Coupon has expired");
        }

        if (coupon.getUsageLimit() != null && coupon.getUsageCount() >= coupon.getUsageLimit()) {
            throw new RuntimeException("Coupon usage limit reached");
        }

        if (orderAmount.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new RuntimeException("Minimum order amount not met for this coupon");
        }

        BigDecimal discount = BigDecimal.ZERO;
        if (coupon.getDiscountType() == Coupon.DiscountType.FIXED) {
            discount = coupon.getDiscountValue();
        } else {
            discount = orderAmount.multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        // Ensure discount doesn't exceed order amount
        return discount.min(orderAmount);
    }

    public void applyCoupon(String code) {
        couponRepository.findByCode(code.toUpperCase().trim()).ifPresent(coupon -> {
            coupon.setUsageCount(coupon.getUsageCount() + 1);
            couponRepository.save(coupon);
        });
    }

    public Coupon createCoupon(Coupon coupon) {
        coupon.setCode(coupon.getCode().toUpperCase().trim());
        return couponRepository.save(coupon);
    }
}
