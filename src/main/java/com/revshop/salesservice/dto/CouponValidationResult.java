package com.revshop.salesservice.dto;

import java.math.BigDecimal;

public class CouponValidationResult {
    private boolean valid;
    private BigDecimal discountAmount;
    private String message;

    public CouponValidationResult() {}

    public CouponValidationResult(boolean valid, BigDecimal discountAmount, String message) {
        this.valid = valid;
        this.discountAmount = discountAmount;
        this.message = message;
    }

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
