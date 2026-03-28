package com.loopers.support;

import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.auth.AuthV1Dto;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

/**
 * E2E 테스트에서 JWT 토큰을 발급받기 위한 헬퍼 클래스.
 */
public final class TestAuthHelper {

    private TestAuthHelper() {}

    /**
     * Auth API를 통해 회원가입하고 Access Token을 반환한다.
     */
    public static String signupAndGetToken(TestRestTemplate restTemplate, String userId, String email, String password) {
        AuthV1Dto.SignupRequest request = new AuthV1Dto.SignupRequest(
                userId, email, "1990-01-01", com.loopers.domain.user.Gender.MALE, password
        );

        ResponseEntity<ApiResponse<AuthV1Dto.AuthResponse>> response =
                restTemplate.exchange(
                        "/api/v1/auth/signup",
                        HttpMethod.POST,
                        new HttpEntity<>(request),
                        new ParameterizedTypeReference<>() {}
                );

        if (response.getBody() != null && response.getBody().data() != null) {
            return response.getBody().data().accessToken();
        }
        throw new RuntimeException("Failed to signup and get token for userId: " + userId);
    }

    /**
     * Auth API를 통해 ADMIN 권한으로 회원가입하고 Access Token을 반환한다.
     * 참고: 실제로는 DB에서 직접 role을 변경해야 하므로 JwtTokenProvider를 직접 사용한다.
     */
    public static String signupAndGetAdminToken(TestRestTemplate restTemplate,
                                                 com.loopers.support.auth.JwtTokenProvider jwtTokenProvider,
                                                 String userId, String email, String password) {
        // 먼저 일반 회원가입
        AuthV1Dto.SignupRequest request = new AuthV1Dto.SignupRequest(
                userId, email, "1990-01-01", com.loopers.domain.user.Gender.MALE, password
        );

        restTemplate.exchange(
                "/api/v1/auth/signup",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<ApiResponse<AuthV1Dto.AuthResponse>>() {}
        );

        // ADMIN 토큰을 직접 발급
        return jwtTokenProvider.createAccessToken(userId, com.loopers.domain.user.Role.ADMIN);
    }

    /**
     * Authorization: Bearer {token} 헤더를 포함한 HttpHeaders를 생성한다.
     */
    public static HttpHeaders authHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        return headers;
    }

    /**
     * Authorization: Bearer {token} + Content-Type: application/json 헤더를 생성한다.
     */
    public static HttpHeaders authJsonHeaders(String accessToken) {
        HttpHeaders headers = authHeaders(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
