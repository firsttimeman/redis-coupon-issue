package com.example.coupon.application;

import com.example.coupon.domain.CouponIssue;
import com.example.coupon.domain.CouponIssueRepository;
import com.example.coupon.domain.CouponIssueStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class CouponIssueRecoveryScheduler {

    private static final Duration PROCESSING_TIMEOUT = Duration.ofMinutes(1);
    private static final long RECOVERY_FIXED_DELAY = 30000;

    private final CouponIssueRepository couponIssueRepository;

    @Transactional
    @Scheduled(fixedDelay = RECOVERY_FIXED_DELAY)
    public void recoverStaleProcessingIssues() {
        LocalDateTime threshold = LocalDateTime.now().minus(PROCESSING_TIMEOUT);

        List<CouponIssue> staleIssues = couponIssueRepository.findStaleProcessingIssues(
                CouponIssueStatus.PROCESSING,
                threshold
        );

        if (staleIssues.isEmpty()) {
            return;
        }

        staleIssues.forEach(CouponIssue::recoverToPending);
        log.warn("Recovered stale processing coupon issues. count={}", staleIssues.size());
    }
}
