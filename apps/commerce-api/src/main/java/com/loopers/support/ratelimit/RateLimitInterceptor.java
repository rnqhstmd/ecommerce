package com.loopers.support.ratelimit;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final String RATE_LIMIT_KEY_PREFIX = "rate:limit:";

    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT;

    static {
        RATE_LIMIT_SCRIPT = new DefaultRedisScript<>();
        RATE_LIMIT_SCRIPT.setScriptText(
                "local count = redis.call('INCR', KEYS[1]) " +
                "if count == 1 then " +
                "  redis.call('EXPIRE', KEYS[1], ARGV[1]) " +
                "end " +
                "return count"
        );
        RATE_LIMIT_SCRIPT.setResultType(Long.class);
    }

    private final RedisTemplate<String, String> redisTemplate;
    private final RateLimitProperties rateLimitProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!rateLimitProperties.enabled()) {
            return true;
        }
        String identifier = resolveIdentifier(request);
        String key = RATE_LIMIT_KEY_PREFIX + identifier;

        Long count = redisTemplate.execute(
                RATE_LIMIT_SCRIPT,
                List.of(key),
                String.valueOf(rateLimitProperties.windowSeconds())
        );

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
        // IP 폴백: X-Forwarded-For는 클라이언트가 조작 가능하므로 remoteAddr만 사용
        return "ip:" + request.getRemoteAddr();
    }
}
