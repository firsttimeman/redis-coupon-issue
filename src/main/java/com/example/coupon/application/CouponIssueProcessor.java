package com.example.coupon.application;

import com.example.coupon.domain.CouponIssue;
import com.example.coupon.domain.CouponIssueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponIssueProcessor {

    private final CouponIssueRepository couponIssueRepository;

    @Transactional
    public void process(Long couponIssueId) {
        int updatedCount = couponIssueRepository.startProcessingIfPending(couponIssueId, LocalDateTime.now());

        if (updatedCount == 0) {
            throw new CouponIssueProcessSkipException(couponIssueId);
        }

        CouponIssue couponIssue = couponIssueRepository.findById(couponIssueId)
                .orElseThrow(() -> new CouponIssueProcessSkipException(couponIssueId));

        String issuedCouponCode = "COUPON-" + couponIssueId;

        couponIssue.complete(issuedCouponCode);

        log.info("Coupon issue completed. couponIssueId={}, issuedCouponCode={}",
                couponIssueId,
                issuedCouponCode);
    }
}
