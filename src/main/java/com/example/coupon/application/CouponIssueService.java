package com.example.coupon.application;

import com.example.coupon.api.CouponIssueCreateRequest;
import com.example.coupon.api.CouponIssueCreateResponse;
import com.example.coupon.api.CouponIssueResponse;
import com.example.coupon.domain.CouponIssue;
import com.example.coupon.domain.CouponIssueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponIssueService {

    private final CouponIssueRepository couponIssueRepository;


    @Transactional
    public CouponIssueCreateResponse create(CouponIssueCreateRequest request) {

        CouponIssue couponIssue = CouponIssue.builder()
                .userId(request.userId())
                .couponName(request.couponName())
                .discountAmount(request.discountAmount())
                .build();

        CouponIssue saved = couponIssueRepository.save(couponIssue);
        return new CouponIssueCreateResponse(
                saved.getId(),
                saved.getStatus()
        );
    }

    @Transactional(readOnly = true)
    public CouponIssueResponse get(Long couponIssueId) {
        CouponIssue couponIssue = couponIssueRepository.findById(couponIssueId)
                .orElseThrow(() -> new IllegalArgumentException("couponIssueId " + couponIssueId + " not found"));

        return CouponIssueResponse.from(couponIssue);
    }


}
