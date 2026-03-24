package com.loopers.support.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rate-limit")
public record RateLimitProperties(
        boolean enabled,
        int maxRequests,
        int windowSeconds
) {
    public RateLimitProperties {
        if (maxRequests <= 0) maxRequests = 60;
        if (windowSeconds <= 0) windowSeconds = 60;
    }
}
