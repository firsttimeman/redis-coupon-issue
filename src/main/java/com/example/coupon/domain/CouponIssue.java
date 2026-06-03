package com.example.coupon.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupon_issue")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String couponName;

    @Column(nullable = false)
    private Integer discountAmount;

    @Column
    private String issuedCouponCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CouponIssueStatus status;

    @Column(nullable = false)
    private int retryCount = 0;

    @Column
    private String errorMessage;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime processedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Builder
    private CouponIssue(Long userId, String couponName, Integer discountAmount) {
        this.userId = userId;
        this.couponName = couponName;
        this.discountAmount = discountAmount;
        this.status = CouponIssueStatus.PENDING;
        this.retryCount = 0;
    }

    public void startProcessing() {
        this.status = CouponIssueStatus.PROCESSING;
    }

    public void complete(String issuedCouponCode) {
        this.issuedCouponCode = issuedCouponCode;
        this.status = CouponIssueStatus.SUCCESS;
        this.processedAt = LocalDateTime.now();
    }

    public boolean isSuccess() {
        return this.status == CouponIssueStatus.SUCCESS;
    }

    public void fail(String errorMessage) {
        this.status = CouponIssueStatus.FAILED;
        this.errorMessage = errorMessage;
        this.retryCount++;
    }
}
