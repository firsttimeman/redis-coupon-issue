package com.example.coupon.api;

import com.example.coupon.application.CouponIssueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/coupon-issues")
public class CouponIssueController {

    private final CouponIssueService couponIssueService;

    @PostMapping
    public ResponseEntity<CouponIssueCreateResponse> createCouponIssue(@RequestBody @Valid
                                                             CouponIssueCreateRequest request) {

        CouponIssueCreateResponse response = couponIssueService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("{couponIssueId}")
    public ResponseEntity<CouponIssueResponse> getCouponIssue(@PathVariable Long couponIssueId) {
        CouponIssueResponse response = couponIssueService.get(couponIssueId);
        return ResponseEntity.ok(response);
    }
}
