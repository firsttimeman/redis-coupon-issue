package com.example.coupon.redis;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "coupon.issue")
// application.yml의 coupon.issue 하위 설정값을 Java 객체로 바인딩한다.
// 1. coupon.issue.stream-key 값을 받을 필드를 선언한다.
// 2. coupon.issue.group 값을 받을 필드를 선언한다.
// 3. coupon.issue.consumer 값을 받을 필드를 선언한다.
public record CouponIssueStreamProperties(
        String streamKey,
        String group,
        String consumer
) {
}
