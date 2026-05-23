package com.example.coupon.api;

import com.example.coupon.domain.CouponIssue;
import com.example.coupon.domain.CouponIssueStatus;

import java.time.LocalDateTime;

public record CouponIssueResponse(
        Long couponIssueId,
        Long userId,
        String couponName,
        Integer discountAmount,
        String issuedCouponCode,
        CouponIssueStatus status,
        int retryCount,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime processedAt
) {
    public static CouponIssueResponse from(CouponIssue couponIssue) {
        return new CouponIssueResponse(
                couponIssue.getId(),
                couponIssue.getUserId(),
                couponIssue.getCouponName(),
                couponIssue.getDiscountAmount(),
                couponIssue.getIssuedCouponCode(),
                couponIssue.getStatus(),
                couponIssue.getRetryCount(),
                couponIssue.getErrorMessage(),
                couponIssue.getCreatedAt(),
                couponIssue.getUpdatedAt(),
                couponIssue.getProcessedAt()
        );
    }
}
