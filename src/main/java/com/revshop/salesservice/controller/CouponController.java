package com.revshop.salesservice.controller;

import com.revshop.salesservice.dto.ApiResponse;
import com.revshop.salesservice.dto.CouponValidationRequest;
import com.revshop.salesservice.dto.CouponValidationResult;
import com.revshop.salesservice.model.Coupon;
import com.revshop.salesservice.service.CouponService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<CouponValidationResult>> validate(@RequestBody CouponValidationRequest request) {
        try {
            BigDecimal discount = couponService.validateAndCalculateDiscount(request.getCode(), request.getOrderAmount());
            return ResponseEntity.ok(new ApiResponse<>("Coupon validated", new CouponValidationResult(true, discount, "Coupon applied successfully")));
        } catch (Exception e) {
            return ResponseEntity.ok(new ApiResponse<>("Coupon invalid", new CouponValidationResult(false, BigDecimal.ZERO, e.getMessage())));
        }
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<Coupon>>> getActiveCoupons() {
        return ResponseEntity.ok(new ApiResponse<>("Active coupons fetched", couponService.getActiveCoupons()));
    }

    @PostMapping("/create")
    public ResponseEntity<Coupon> create(@RequestBody Coupon coupon) {
        return ResponseEntity.ok(couponService.createCoupon(coupon));
    }
}
