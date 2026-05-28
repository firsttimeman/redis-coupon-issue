package com.example.coupon.application;

import com.example.coupon.api.CouponIssueCreateRequest;
import com.example.coupon.api.CouponIssueCreateResponse;
import com.example.coupon.api.CouponIssueResponse;
import com.example.coupon.domain.CouponIssue;
import com.example.coupon.domain.CouponIssueRepository;
import com.example.coupon.exception.CouponIssueNotFoundException;
import com.example.coupon.redis.CouponIssueStreamProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponIssueService {

    private final CouponIssueRepository couponIssueRepository;
    private final CouponIssueStreamProducer couponIssueStreamProducer;


    @Transactional
    public CouponIssueCreateResponse create(CouponIssueCreateRequest request) {

        CouponIssue couponIssue = CouponIssue.builder()
                .userId(request.userId())
                .couponName(request.couponName())
                .discountAmount(request.discountAmount())
                .build();

        CouponIssue saved = couponIssueRepository.save(couponIssue);
        log.info("Saved coupon issue. couponIssueId={}", saved.getId());

        couponIssueStreamProducer.publish(saved);

        return new CouponIssueCreateResponse(
                saved.getId(),
                saved.getStatus()
        );
    }

    @Transactional(readOnly = true)
    public CouponIssueResponse get(Long couponIssueId) {
        CouponIssue couponIssue = couponIssueRepository.findById(couponIssueId)
                .orElseThrow(() -> new CouponIssueNotFoundException(couponIssueId));

        return CouponIssueResponse.from(couponIssue);
    }


}
