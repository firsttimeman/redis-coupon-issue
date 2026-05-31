package com.example.coupon.redis;

import com.example.coupon.application.CouponIssueProcessSkipException;
import com.example.coupon.application.CouponIssueProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueStreamWorker {

    // Redis Stream에서 메시지를 읽고 ACK 하기 위해 사용한다.
    private final StringRedisTemplate stringRedisTemplate;

    // streamKey, group, consumer 이름은 application.yml의 coupon.issue 설정을 사용한다.
    private final CouponIssueStreamProperties properties;

    // 메시지 안의 couponIssueId를 실제 쿠폰 발급 처리로 연결한다.
    private final CouponIssueProcessor couponIssueProcessor;

    @Scheduled(fixedDelay = 1000)
    public void poll() {
        String streamKey = properties.streamKey();
        String group = properties.group();
        String consumer = properties.consumer();

        List<MapRecord<String, Object, Object>> records =
                stringRedisTemplate.opsForStream().read(
                        Consumer.from(group, consumer),
                        StreamReadOptions.empty()
                                .count(1)
                                .block(Duration.ofSeconds(1)),
                        StreamOffset.create(streamKey, ReadOffset.lastConsumed())
                );

        if (records == null || records.isEmpty()) {
            return;
        }

        MapRecord<String, Object, Object> record = records.get(0);
        Long couponIssueId;

        try {
            couponIssueId = extractCouponIssueId(record);
        } catch (IllegalArgumentException exception) {
            log.warn("Invalid coupon issue message. recordId={}, values={}",
                    record.getId(),
                    record.getValue(),
                    exception);
            acknowledge(streamKey, group, record);
            return;
        }

        try {
            couponIssueProcessor.process(couponIssueId);
            acknowledge(streamKey, group, record);
        } catch (CouponIssueProcessSkipException exception) {
            log.warn("Skipping coupon issue message. recordId={}, couponIssueId={}",
                    record.getId(),
                    couponIssueId,
                    exception);
            acknowledge(streamKey, group, record);
        } catch (Exception exception) {
            log.error("Failed to process coupon issue message. recordId={}, couponIssueId={}",
                    record.getId(),
                    couponIssueId,
                    exception);
            throw exception;
        }
    }

    private Long extractCouponIssueId(MapRecord<String, Object, Object> record) {
        Object couponIssueIdValue = record.getValue().get("couponIssueId");

        if (couponIssueIdValue == null) {
            throw new IllegalArgumentException("couponIssueId is missing. recordId=" + record.getId());
        }

        return Long.valueOf(String.valueOf(couponIssueIdValue));
    }

    private void acknowledge(String streamKey, String group, MapRecord<String, Object, Object> record) {
        Long acknowledgedCount = stringRedisTemplate.opsForStream()
                .acknowledge(streamKey, group, record.getId());

        log.info("Coupon issue message acknowledged. recordId={}, acknowledgedCount={}",
                record.getId(),
                acknowledgedCount);
    }
}
