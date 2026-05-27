package com.example.coupon.exception;

public class CouponIssueNotFoundException extends RuntimeException {

    public CouponIssueNotFoundException(Long couponIssueId) {
        super("couponIssueId " + couponIssueId + " not found");
    }
}
