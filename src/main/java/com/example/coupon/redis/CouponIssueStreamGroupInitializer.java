package com.example.coupon.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueStreamGroupInitializer implements ApplicationRunner {

    // Redis Stream의 Consumer Group을 만들기 위해 StringRedisTemplate을 사용한다.
    private final StringRedisTemplate stringRedisTemplate;

    // streamKey, group 이름은 application.yml의 coupon.issue 설정을 사용한다.
    private final CouponIssueStreamProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        String streamKey = properties.streamKey();
        String group = properties.group();

        log.info("Initializing Redis Stream group. streamKey={}, group={}", streamKey, group);

        try {
            stringRedisTemplate.execute((RedisCallback<Object>) connection -> {
                // XGROUP CREATE <streamKey> <group> 0 MKSTREAM
                connection.streamCommands().xGroupCreate(
                        streamKey.getBytes(StandardCharsets.UTF_8),
                        group,
                        ReadOffset.from("0"),
                        true
                );
                return null;
            });

            log.info("Redis Stream group created. streamKey={}, group={}", streamKey, group);
        }
        catch (RedisSystemException exception) {
            if (exception.getMessage() != null && exception.getMessage().contains("BUSYGROUP")) {
                log.info("Redis Stream group already exists. streamKey={}, group={}", streamKey, group);
                return;
            }

            throw exception;
        }
    }
}
