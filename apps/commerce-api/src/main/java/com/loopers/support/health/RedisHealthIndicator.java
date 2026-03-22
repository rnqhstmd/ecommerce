package com.loopers.support.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisHealthIndicator implements HealthIndicator {

    private final RedisConnectionFactory redisConnectionFactory;

    @Override
    public Health health() {
        try {
            String pong = redisConnectionFactory.getConnection().ping();
            if ("PONG".equals(pong)) {
                return Health.up()
                        .withDetail("redis", "연결 정상")
                        .build();
            }
            return Health.down()
                    .withDetail("redis", "PING 응답 비정상: " + pong)
                    .build();
        } catch (Exception e) {
            log.warn("Redis health check 실패: {}", e.getMessage());
            return Health.down()
                    .withDetail("redis", "연결 실패")
                    .withException(e)
                    .build();
        }
    }
}
