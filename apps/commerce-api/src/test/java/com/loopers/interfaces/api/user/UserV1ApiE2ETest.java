package com.loopers.interfaces.api.user;

import com.loopers.domain.user.Gender;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.auth.AuthV1Dto;
import com.loopers.support.TestAuthHelper;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserV1ApiE2ETest {

    private static final String ENDPOINT_SIGN_UP = "/api/v1/users";
    private static final String ENDPOINT_GET_USER = "/api/v1/users/me";

    private final TestRestTemplate testRestTemplate;
    private final DatabaseCleanUp databaseCleanUp;

    @Autowired
    public UserV1ApiE2ETest(
            TestRestTemplate testRestTemplate,
            DatabaseCleanUp databaseCleanUp
    ) {
        this.testRestTemplate = testRestTemplate;
        this.databaseCleanUp = databaseCleanUp;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("POST /api/v1/users")
    @Nested
    class SignUp {

        @DisplayName("회원 가입 후 내 정보 조회에 성공할 경우, 가입한 유저 정보를 응답으로 반환한다.")
        @Test
        void returnsUserInfo_whenSignUpIsSuccessful() {
            // arrange - auth API로 회원가입하여 토큰 발급
            String token = TestAuthHelper.signupAndGetToken(testRestTemplate, "testuser01", "test@example.com", "password123");

            // act - 내 정보 조회로 가입 확인
            HttpHeaders headers = TestAuthHelper.authHeaders(token);
            ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> response =
                    testRestTemplate.exchange(
                            ENDPOINT_GET_USER,
                            HttpMethod.GET,
                            new HttpEntity<>(headers),
                            responseType
                    );

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data()).isNotNull(),
                    () -> assertThat(response.getBody().data().userId()).isEqualTo("testuser01"),
                    () -> assertThat(response.getBody().data().email()).isEqualTo("test@example.com")
            );
        }

        @DisplayName("회원 가입 시 성별이 없을 경우, 400 Bad Request 응답을 반환한다.")
        @Test
        void returnsBadRequest_whenGenderIsMissing() {
            // arrange
            String requestBody = """
                    {
                    	"userId": "testuser01",
                    	"email": "test@example.com",
                    	"birthDate": "1990-01-01",
                    	"password": "password123"
                    }
                    """;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // act
            ParameterizedTypeReference<ApiResponse<AuthV1Dto.AuthResponse>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<AuthV1Dto.AuthResponse>> response =
                    testRestTemplate.exchange(
                            "/api/v1/auth/signup",
                            HttpMethod.POST,
                            new HttpEntity<>(requestBody, headers),
                            responseType
                    );

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
            );
        }

        @DisplayName("회원 가입 시 ID가 형식에 맞지 않으면, 400 Bad Request 응답을 반환한다.")
        @Test
        void returnsBadRequest_whenUserIdFormatIsInvalid() {
            // arrange
            AuthV1Dto.SignupRequest request = new AuthV1Dto.SignupRequest(
                    "invalid-id!",
                    "test@example.com",
                    "1990-01-01",
                    Gender.MALE,
                    "password123"
            );

            // act
            ParameterizedTypeReference<ApiResponse<AuthV1Dto.AuthResponse>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<AuthV1Dto.AuthResponse>> response =
                    testRestTemplate.exchange(
                            "/api/v1/auth/signup",
                            HttpMethod.POST,
                            new HttpEntity<>(request),
                            responseType
                    );

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
            );
        }

        @DisplayName("이미 가입된 ID로 회원 가입 시도 시, 409 Conflict 응답을 반환한다.")
        @Test
        void returnsConflict_whenUserIdAlreadyExists() {
            // arrange - 첫 번째 회원 가입
            TestAuthHelper.signupAndGetToken(testRestTemplate, "testuser01", "test@example.com", "password123");

            // 두 번째 회원 가입 시도 (같은 ID)
            AuthV1Dto.SignupRequest secondRequest = new AuthV1Dto.SignupRequest(
                    "testuser01",
                    "another@example.com",
                    "1995-05-05",
                    Gender.FEMALE,
                    "password456"
            );

            // act
            ParameterizedTypeReference<ApiResponse<AuthV1Dto.AuthResponse>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<AuthV1Dto.AuthResponse>> response =
                    testRestTemplate.exchange(
                            "/api/v1/auth/signup",
                            HttpMethod.POST,
                            new HttpEntity<>(secondRequest),
                            responseType
                    );

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT)
            );
        }
    }

    @DisplayName("GET /api/v1/users/me")
    @Nested
    class GetUser {

        @DisplayName("내 정보 조회에 성공할 경우, 해당하는 유저 정보를 응답으로 반환한다.")
        @Test
        void returnsUserInfo_whenUserExists() {
            // arrange - 회원 가입
            String token = TestAuthHelper.signupAndGetToken(testRestTemplate, "testuser01", "test@example.com", "password123");

            // act - 내 정보 조회
            HttpHeaders headers = TestAuthHelper.authHeaders(token);
            HttpEntity<?> requestEntity = new HttpEntity<>(headers);

            ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> response =
                    testRestTemplate.exchange(
                            ENDPOINT_GET_USER,
                            HttpMethod.GET,
                            requestEntity,
                            responseType
                    );

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data()).isNotNull(),
                    () -> assertThat(response.getBody().data().userId()).isEqualTo("testuser01"),
                    () -> assertThat(response.getBody().data().email()).isEqualTo("test@example.com")
            );
        }

        @DisplayName("토큰 없이 조회할 경우, 401 Unauthorized 응답을 반환한다.")
        @Test
        void returnsUnauthorized_whenNoToken() {
            // act
            ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> response =
                    testRestTemplate.exchange(
                            ENDPOINT_GET_USER,
                            HttpMethod.GET,
                            null,
                            responseType
                    );

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED)
            );
        }
    }
}
