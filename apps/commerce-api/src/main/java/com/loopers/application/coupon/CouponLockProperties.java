package com.loopers.application.coupon;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "coupon.lock")
public record CouponLockProperties(
        long waitTimeSeconds,
        long leaseTimeSeconds
) {
    public CouponLockProperties {
        if (waitTimeSeconds <= 0) waitTimeSeconds = 3;
        if (leaseTimeSeconds <= 0) leaseTimeSeconds = 60;
    }
}
