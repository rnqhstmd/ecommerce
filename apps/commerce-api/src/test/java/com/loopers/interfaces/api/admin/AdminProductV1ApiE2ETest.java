package com.loopers.interfaces.api.admin;

import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.TestAuthHelper;
import com.loopers.support.auth.JwtTokenProvider;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
class AdminProductV1ApiE2ETest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        adminToken = TestAuthHelper.signupAndGetAdminToken(
                testRestTemplate, jwtTokenProvider,
                "admridx", "admridx@example.com", "password123"
        );
        userToken = TestAuthHelper.signupAndGetToken(
                testRestTemplate, "usrridx", "usrridx@example.com", "password123"
        );
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("POST /api/v1/admin/products/reindex - ADMIN 권한으로 재인덱싱에 성공한다.")
    @Test
    void reindex_success_whenAdmin() {
        // arrange
        HttpHeaders headers = TestAuthHelper.authHeaders(adminToken);

        // act
        ResponseEntity<ApiResponse<AdminProductV1Dto.ReindexResponse>> response =
                testRestTemplate.exchange(
                        "/api/v1/admin/products/reindex",
                        HttpMethod.POST,
                        new HttpEntity<>(headers),
                        new ParameterizedTypeReference<>() {}
                );

        // assert
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().data()).isNotNull(),
                () -> assertThat(response.getBody().data().indexed()).isGreaterThanOrEqualTo(0)
        );
    }

    @DisplayName("POST /api/v1/admin/products/reindex - USER 권한이면 403을 반환한다.")
    @Test
    void reindex_returnsForbidden_whenUser() {
        // arrange
        HttpHeaders headers = TestAuthHelper.authHeaders(userToken);

        // act
        ResponseEntity<ApiResponse<Object>> response =
                testRestTemplate.exchange(
                        "/api/v1/admin/products/reindex",
                        HttpMethod.POST,
                        new HttpEntity<>(headers),
                        new ParameterizedTypeReference<>() {}
                );

        // assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @DisplayName("POST /api/v1/admin/products/reindex - 미인증 요청이면 401을 반환한다.")
    @Test
    void reindex_returnsUnauthorized_whenNoAuth() {
        // act
        ResponseEntity<ApiResponse<Object>> response =
                testRestTemplate.exchange(
                        "/api/v1/admin/products/reindex",
                        HttpMethod.POST,
                        null,
                        new ParameterizedTypeReference<>() {}
                );

        // assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
