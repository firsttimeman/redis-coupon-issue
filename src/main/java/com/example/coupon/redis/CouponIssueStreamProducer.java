package com.example.coupon.redis;

import com.example.coupon.domain.CouponIssue;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class CouponIssueStreamProducer {

    // Redis Stream에 문자열 기반 메시지를 추가하기 위해 StringRedisTemplate을 주입한다.
    private final StringRedisTemplate stringRedisTemplate;

    // Stream 이름을 application.yml에서 가져오기 위해 CouponIssueStreamProperties를 주입한다.
    private final CouponIssueStreamProperties properties;

    public RecordId publish(CouponIssue couponIssue) {
        // couponIssue에서 Redis Stream에 넣을 메시지 필드를 만든다.
        // 필요한 필드: couponIssueId, userId, couponName, createdAt

        Map<String,String> message = Map.of(
                "couponIssueId", String.valueOf(couponIssue.getId()),
                "userId", String.valueOf(couponIssue.getUserId()),
                "couponName", couponIssue.getCouponName(),
                "createdAt", String.valueOf(couponIssue.getCreatedAt())
        );

        // StringRedisTemplate의 opsForStream().add(...)를 사용해서 Stream에 메시지를 추가한다.
        // Stream key는 CouponIssueStreamProperties에서 가져온 값을 사용한다.
        MapRecord<String, String, String> record = StreamRecords.newRecord()
                .ofMap(message)
                .withStreamKey(properties.streamKey());


        // Redis가 생성한 메시지 ID인 RecordId를 반환한다.
        return stringRedisTemplate.opsForStream().add(record);
    }
}
