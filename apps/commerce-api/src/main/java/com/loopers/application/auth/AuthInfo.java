package com.loopers.application.auth;

public record AuthInfo(
        String accessToken,
        String refreshToken,
        String userId
) {
}
