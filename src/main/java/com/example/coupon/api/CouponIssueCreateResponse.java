package com.example.coupon.api;

import com.example.coupon.domain.CouponIssueStatus;

public record CouponIssueCreateResponse(
        Long couponIssueId,
        CouponIssueStatus status
) {
}
