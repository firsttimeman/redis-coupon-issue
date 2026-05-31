package com.example.coupon.config;

import com.example.coupon.redis.CouponIssueStreamProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CouponIssueStreamProperties.class)
public class RedisConfig {

    // CouponIssueStreamProperties를 Spring Bean으로 등록하기 위한 설정 클래스다.
    // 지금 단계에서는 별도 RedisTemplate Bean을 직접 만들 필요는 없다.
    // Spring Boot가 spring.data.redis 설정을 보고 StringRedisTemplate을 자동으로 준비한다.

}
