package com.loopers.support.ratelimit;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final String RATE_LIMIT_KEY_PREFIX = "rate:limit:";

    private final RedisTemplate<String, String> redisTemplate;
    private final RateLimitProperties rateLimitProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!rateLimitProperties.enabled()) {
            return true;
        }
        String identifier = resolveIdentifier(request);
        String key = RATE_LIMIT_KEY_PREFIX + identifier;

        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, rateLimitProperties.windowSeconds(), TimeUnit.SECONDS);
        }

        if (count != null && count > rateLimitProperties.maxRequests()) {
            throw new CoreException(ErrorType.TOO_MANY_REQUESTS);
        }

        return true;
    }

    private String resolveIdentifier(HttpServletRequest request) {
        String userId = request.getHeader("X-USER-ID");
        if (userId != null && !userId.isBlank()) {
            return "user:" + userId;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return "ip:" + forwarded.split(",")[0].trim();
        }
        return "ip:" + request.getRemoteAddr();
    }
}
