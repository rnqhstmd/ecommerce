package com.loopers.interfaces.api.auth;

import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.TestAuthHelper;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthV1ApiE2ETest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("POST /api/v1/auth/signup - 회원가입 성공 시 accessToken과 refreshToken을 반환한다.")
    @Test
    void signup_success() {
        // arrange
        AuthV1Dto.SignupRequest request = new AuthV1Dto.SignupRequest(
                "newuser01", "new@example.com", "1990-01-01", "MALE", "password123"
        );

        // act
        ResponseEntity<ApiResponse<AuthV1Dto.AuthResponse>> response =
                testRestTemplate.exchange(
                        "/api/v1/auth/signup",
                        HttpMethod.POST,
                        new HttpEntity<>(request),
                        new ParameterizedTypeReference<>() {}
                );

        // assert
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().data()).isNotNull(),
                () -> assertThat(response.getBody().data().accessToken()).isNotBlank(),
                () -> assertThat(response.getBody().data().refreshToken()).isNotBlank(),
                () -> assertThat(response.getBody().data().userId()).isEqualTo("newuser01")
        );
    }

    @DisplayName("POST /api/v1/auth/signup - 중복 userId로 가입 시 409를 반환한다.")
    @Test
    void signup_returnsConflict_whenUserIdDuplicated() {
        // arrange - 첫 번째 가입
        AuthV1Dto.SignupRequest firstRequest = new AuthV1Dto.SignupRequest(
                "dupuser01", "dup1@example.com", "1990-01-01", "MALE", "password123"
        );
        testRestTemplate.exchange(
                "/api/v1/auth/signup",
                HttpMethod.POST,
                new HttpEntity<>(firstRequest),
                new ParameterizedTypeReference<ApiResponse<AuthV1Dto.AuthResponse>>() {}
        );

        // 두 번째 가입 (같은 userId)
        AuthV1Dto.SignupRequest secondRequest = new AuthV1Dto.SignupRequest(
                "dupuser01", "dup2@example.com", "1995-05-05", "FEMALE", "password456"
        );

        // act
        ResponseEntity<ApiResponse<Object>> response =
                testRestTemplate.exchange(
                        "/api/v1/auth/signup",
                        HttpMethod.POST,
                        new HttpEntity<>(secondRequest),
                        new ParameterizedTypeReference<>() {}
                );

        // assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @DisplayName("POST /api/v1/auth/login - 올바른 자격증명으로 로그인 성공한다.")
    @Test
    void login_success() {
        // arrange - 회원가입
        AuthV1Dto.SignupRequest signupRequest = new AuthV1Dto.SignupRequest(
                "loginuser", "login@example.com", "1990-01-01", "MALE", "password123"
        );
        testRestTemplate.exchange(
                "/api/v1/auth/signup",
                HttpMethod.POST,
                new HttpEntity<>(signupRequest),
                new ParameterizedTypeReference<ApiResponse<AuthV1Dto.AuthResponse>>() {}
        );

        // act - 로그인
        AuthV1Dto.LoginRequest loginRequest = new AuthV1Dto.LoginRequest("loginuser", "password123");
        ResponseEntity<ApiResponse<AuthV1Dto.AuthResponse>> response =
                testRestTemplate.exchange(
                        "/api/v1/auth/login",
                        HttpMethod.POST,
                        new HttpEntity<>(loginRequest),
                        new ParameterizedTypeReference<>() {}
                );

        // assert
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().data().accessToken()).isNotBlank(),
                () -> assertThat(response.getBody().data().refreshToken()).isNotBlank(),
                () -> assertThat(response.getBody().data().userId()).isEqualTo("loginuser")
        );
    }

    @DisplayName("POST /api/v1/auth/login - 잘못된 비밀번호로 로그인 시 401을 반환한다.")
    @Test
    void login_returnsUnauthorized_whenPasswordIsWrong() {
        // arrange - 회원가입
        AuthV1Dto.SignupRequest signupRequest = new AuthV1Dto.SignupRequest(
                "wrongpw", "wrong@example.com", "1990-01-01", "MALE", "password123"
        );
        testRestTemplate.exchange(
                "/api/v1/auth/signup",
                HttpMethod.POST,
                new HttpEntity<>(signupRequest),
                new ParameterizedTypeReference<ApiResponse<AuthV1Dto.AuthResponse>>() {}
        );

        // act - 잘못된 비밀번호로 로그인
        AuthV1Dto.LoginRequest loginRequest = new AuthV1Dto.LoginRequest("wrongpw", "wrongpassword");
        ResponseEntity<ApiResponse<Object>> response =
                testRestTemplate.exchange(
                        "/api/v1/auth/login",
                        HttpMethod.POST,
                        new HttpEntity<>(loginRequest),
                        new ParameterizedTypeReference<>() {}
                );

        // assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @DisplayName("POST /api/v1/auth/refresh - 유효한 refreshToken으로 토큰 갱신에 성공한다.")
    @Test
    void refresh_success() {
        // arrange - 회원가입하여 refreshToken 획득
        AuthV1Dto.SignupRequest signupRequest = new AuthV1Dto.SignupRequest(
                "refresh01", "refresh@example.com", "1990-01-01", "MALE", "password123"
        );
        ResponseEntity<ApiResponse<AuthV1Dto.AuthResponse>> signupResponse =
                testRestTemplate.exchange(
                        "/api/v1/auth/signup",
                        HttpMethod.POST,
                        new HttpEntity<>(signupRequest),
                        new ParameterizedTypeReference<>() {}
                );
        String refreshToken = signupResponse.getBody().data().refreshToken();

        // act - 토큰 갱신
        AuthV1Dto.RefreshRequest refreshRequest = new AuthV1Dto.RefreshRequest(refreshToken);
        ResponseEntity<ApiResponse<AuthV1Dto.AuthResponse>> response =
                testRestTemplate.exchange(
                        "/api/v1/auth/refresh",
                        HttpMethod.POST,
                        new HttpEntity<>(refreshRequest),
                        new ParameterizedTypeReference<>() {}
                );

        // assert
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().data().accessToken()).isNotBlank(),
                () -> assertThat(response.getBody().data().refreshToken()).isNotBlank(),
                () -> assertThat(response.getBody().data().userId()).isEqualTo("refresh01")
        );
    }

    @DisplayName("인증 없이 보호된 API 접근 시 401을 반환한다.")
    @Test
    void protectedApi_returnsUnauthorized_whenNoToken() {
        // act - 토큰 없이 보호된 API (내 정보 조회) 접근
        ResponseEntity<ApiResponse<Object>> response =
                testRestTemplate.exchange(
                        "/api/v1/users/me",
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<>() {}
                );

        // assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
