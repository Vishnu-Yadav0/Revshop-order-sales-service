package com.revshop.salesservice.controller;

import com.revshop.salesservice.model.Coupon;
import com.revshop.salesservice.service.CouponService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @GetMapping("/validate")
    public ResponseEntity<BigDecimal> validate(@RequestParam String code, @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(couponService.validateAndCalculateDiscount(code, amount));
    }

    @PostMapping("/create")
    public ResponseEntity<Coupon> create(@RequestBody Coupon coupon) {
        return ResponseEntity.ok(couponService.createCoupon(coupon));
    }
}
