package com.example.coupon.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CouponIssueCreateRequest(
        @NotNull
        Long userId,
        @NotBlank
        String couponName,
        @NotNull @Min(1)
        Integer discountAmount
) {
}
