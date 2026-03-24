package com.loopers.support.auth;

import com.loopers.domain.user.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static final String SECRET = "Y29tbWVyY2UtYXBpLXNlY3JldC1rZXktZm9yLWp3dC10b2tlbi1nZW5lcmF0aW9uLTIwMjQ=";
    private static final long ACCESS_EXPIRATION = 1800000L;   // 30분
    private static final long REFRESH_EXPIRATION = 604800000L; // 7일

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(SECRET, ACCESS_EXPIRATION, REFRESH_EXPIRATION);
    }

    @DisplayName("Access Token 생성 시 userId와 role이 올바르게 포함된다.")
    @Test
    void createAccessToken_containsUserIdAndRole() {
        // act
        String token = jwtTokenProvider.createAccessToken("testuser", Role.USER);

        // assert
        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.getUserId(token)).isEqualTo("testuser");
        assertThat(jwtTokenProvider.getRole(token)).isEqualTo(Role.USER);
    }

    @DisplayName("ADMIN 역할로 Access Token을 생성하면 ADMIN role이 포함된다.")
    @Test
    void createAccessToken_withAdminRole() {
        // act
        String token = jwtTokenProvider.createAccessToken("adminuser", Role.ADMIN);

        // assert
        assertThat(jwtTokenProvider.getRole(token)).isEqualTo(Role.ADMIN);
    }

    @DisplayName("Refresh Token 생성 시 userId가 올바르게 포함된다.")
    @Test
    void createRefreshToken_containsUserId() {
        // act
        String token = jwtTokenProvider.createRefreshToken("testuser");

        // assert
        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.getUserId(token)).isEqualTo("testuser");
    }

    @DisplayName("유효한 토큰 검증 시 true를 반환한다.")
    @Test
    void validateToken_returnsTrue_whenTokenIsValid() {
        // arrange
        String token = jwtTokenProvider.createAccessToken("testuser", Role.USER);

        // act & assert
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
    }

    @DisplayName("만료된 토큰 검증 시 false를 반환한다.")
    @Test
    void validateToken_returnsFalse_whenTokenIsExpired() {
        // arrange - 만료 시간이 0인 provider 생성
        JwtTokenProvider expiredProvider = new JwtTokenProvider(SECRET, 0L, 0L);
        String token = expiredProvider.createAccessToken("testuser", Role.USER);

        // act & assert
        assertThat(jwtTokenProvider.validateToken(token)).isFalse();
    }

    @DisplayName("잘못된 형식의 토큰 검증 시 false를 반환한다.")
    @Test
    void validateToken_returnsFalse_whenTokenIsMalformed() {
        // act & assert
        assertThat(jwtTokenProvider.validateToken("invalid.token.value")).isFalse();
    }

    @DisplayName("다른 시크릿 키로 서명된 토큰 검증 시 false를 반환한다.")
    @Test
    void validateToken_returnsFalse_whenSignedWithDifferentKey() {
        // arrange - 다른 키로 토큰 생성
        SecretKey otherKey = Keys.hmacShaKeyFor(
                "another-secret-key-that-is-long-enough-for-hmac-sha256-algorithm!!"
                        .getBytes(StandardCharsets.UTF_8)
        );
        String token = Jwts.builder()
                .subject("testuser")
                .claim("role", "USER")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(otherKey)
                .compact();

        // act & assert
        assertThat(jwtTokenProvider.validateToken(token)).isFalse();
    }

    @DisplayName("null 또는 빈 문자열 토큰 검증 시 false를 반환한다.")
    @Test
    void validateToken_returnsFalse_whenTokenIsNullOrEmpty() {
        // act & assert
        assertThat(jwtTokenProvider.validateToken(null)).isFalse();
        assertThat(jwtTokenProvider.validateToken("")).isFalse();
    }
}
