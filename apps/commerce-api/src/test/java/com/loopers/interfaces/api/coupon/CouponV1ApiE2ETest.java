package com.loopers.interfaces.api.coupon;

import com.loopers.domain.coupon.CouponPolicy;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.coupon.DiscountType;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.TestAuthHelper;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CouponV1ApiE2ETest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private CouponService couponService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String USER_ID = "couponuser";
    private Long couponPolicyId;
    private String accessToken;

    @BeforeEach
    void setUp() {
        // 사용자 생성 (auth API로)
        accessToken = TestAuthHelper.signupAndGetToken(testRestTemplate, USER_ID, "coupon@example.com", "password123");

        // 쿠폰 정책 생성
        CouponPolicy policy = CouponPolicy.create(
                "테스트 10% 할인",
                DiscountType.RATE,
                10L,
                ZonedDateTime.now().minusDays(1),
                ZonedDateTime.now().plusDays(30),
                100
        );
        CouponPolicy savedPolicy = couponService.saveCouponPolicy(policy);
        couponPolicyId = savedPolicy.getId();
    }

    @AfterEach
    void tearDown() {
        redisTemplate.delete("coupon:stock:" + couponPolicyId);
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("POST /api/v1/coupons/{id}/issue - 쿠폰 발급에 성공한다.")
    @Test
    void issueCoupon_success() {
        // arrange
        HttpHeaders headers = TestAuthHelper.authHeaders(accessToken);

        // act
        ResponseEntity<String> response =
                testRestTemplate.exchange(
                        "/api/v1/coupons/" + couponPolicyId + "/issue",
                        HttpMethod.POST,
                        new HttpEntity<>(headers),
                        String.class
                );

        // assert
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody()).contains("\"userCouponId\""),
                () -> assertThat(response.getBody()).contains("\"couponPolicyId\""),
                () -> assertThat(response.getBody()).contains("\"couponName\""),
                () -> assertThat(response.getBody()).contains("테스트 10% 할인")
        );
    }

    @DisplayName("POST /api/v1/coupons/{id}/issue - 토큰 없으면 401을 반환한다.")
    @Test
    void issueCoupon_returnsUnauthorized_whenNoToken() {
        // act
        ResponseEntity<String> response =
                testRestTemplate.exchange(
                        "/api/v1/coupons/" + couponPolicyId + "/issue",
                        HttpMethod.POST,
                        new HttpEntity<>(null, new HttpHeaders()),
                        String.class
                );

        // assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
