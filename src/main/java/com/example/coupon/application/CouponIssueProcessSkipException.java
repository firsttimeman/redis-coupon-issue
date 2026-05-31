package com.example.coupon.application;

public class CouponIssueProcessSkipException extends RuntimeException {

    public CouponIssueProcessSkipException(Long couponIssueId) {
        super("Coupon issue not found. couponIssueId=" + couponIssueId);
    }
}
