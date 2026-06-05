package com.example.coupon.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CouponIssueRepository extends JpaRepository<CouponIssue, Long> {

    @Query("""
        select c
          from CouponIssue c
         where c.status = :status
           and c.updatedAt < :updatedAt
    """)
    List<CouponIssue> findStaleProcessingIssues(@Param("status") CouponIssueStatus status,
                                                @Param("updatedAt") LocalDateTime updatedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update CouponIssue c
           set c.status = com.example.coupon.domain.CouponIssueStatus.PROCESSING,
               c.updatedAt = :updatedAt
         where c.id = :couponIssueId
           and c.status = com.example.coupon.domain.CouponIssueStatus.PENDING
    """)
    int startProcessingIfPending(@Param("couponIssueId") Long couponIssueId,
                                 @Param("updatedAt") LocalDateTime updatedAt);
}
