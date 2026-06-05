package com.example.coupon.application;

import com.example.coupon.domain.CouponIssue;
import com.example.coupon.domain.CouponIssueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponIssueFailureHandler {

    private static final int MAX_RETRY_COUNT = 3;

    private final CouponIssueRepository couponIssueRepository;

    @Transactional
    public boolean recordFailure(Long couponIssueId, Exception exception) {
        CouponIssue couponIssue = couponIssueRepository.findById(couponIssueId)
                .orElse(null);

        if (couponIssue == null) {
            log.warn("Coupon issue not found while recording failure. couponIssueId={}", couponIssueId);
            return true;
        }

        if (couponIssue.isFinished()) {
            log.info("Coupon issue already finished. couponIssueId={}, status={}",
                    couponIssueId,
                    couponIssue.getStatus());
            return true;
        }

        String message = exception.getMessage();
        if (message == null) {
            message = exception.getClass().getSimpleName();
        }

        couponIssue.recordFailure(message, MAX_RETRY_COUNT);
        log.warn("Coupon issue failure recorded. couponIssueId={}, status={}, retryCount={}",
                couponIssueId,
                couponIssue.getStatus(),
                couponIssue.getRetryCount());

        return couponIssue.isFailed();
    }
}
