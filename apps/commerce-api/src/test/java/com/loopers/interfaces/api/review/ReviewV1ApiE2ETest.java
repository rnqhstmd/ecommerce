package com.loopers.interfaces.api.review;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.order.OrderV1Dto;
import com.loopers.support.TestAuthHelper;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReviewV1ApiE2ETest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PointService pointService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private static final String USER_ID = "reviewuser";
    private Long productId;
    private Long orderId;
    private String accessToken;

    @BeforeEach
    void setUp() {
        // 사용자 생성 (auth API로)
        accessToken = TestAuthHelper.signupAndGetToken(testRestTemplate, USER_ID, "review@example.com", "password123");

        // 포인트 충전
        pointService.chargePoint(USER_ID, 1000000L);

        // 상품 생성
        Brand brand = brandRepository.save(Brand.create("Review Brand"));
        Product product = productRepository.save(Product.create("Review Product", 5000L, 100, brand.getId()));
        productId = product.getId();

        // 주문 생성 (PAID 상태)
        HttpHeaders headers = TestAuthHelper.authJsonHeaders(accessToken);
        OrderV1Dto.PlaceOrderRequest orderRequest = new OrderV1Dto.PlaceOrderRequest(
                List.of(new OrderV1Dto.OrderItemRequest(productId, 1)),
                null
        );
        ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> orderResponse =
                testRestTemplate.exchange(
                        "/api/v1/orders",
                        HttpMethod.POST,
                        new HttpEntity<>(orderRequest, headers),
                        new ParameterizedTypeReference<>() {}
                );
        orderId = orderResponse.getBody().data().orderId();
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("POST /api/v1/reviews - 리뷰 작성에 성공한다.")
    @Test
    void createReview_success() {
        // arrange
        HttpHeaders headers = TestAuthHelper.authJsonHeaders(accessToken);
        ReviewV1Dto.CreateReviewRequest request = new ReviewV1Dto.CreateReviewRequest(
                orderId, productId, 5, "정말 좋은 상품입니다!"
        );

        // act
        ResponseEntity<String> response =
                testRestTemplate.exchange(
                        "/api/v1/reviews",
                        HttpMethod.POST,
                        new HttpEntity<>(request, headers),
                        String.class
                );

        // assert
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody()).contains("\"reviewId\""),
                () -> assertThat(response.getBody()).contains("\"rating\":5"),
                () -> assertThat(response.getBody()).contains("정말 좋은 상품입니다!")
        );
    }

    @DisplayName("POST /api/v1/reviews - 토큰 없으면 401을 반환한다.")
    @Test
    void createReview_returnsUnauthorized_whenNoToken() {
        // arrange
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ReviewV1Dto.CreateReviewRequest request = new ReviewV1Dto.CreateReviewRequest(
                orderId, productId, 5, "좋은 상품입니다."
        );

        // act
        ResponseEntity<String> response =
                testRestTemplate.exchange(
                        "/api/v1/reviews",
                        HttpMethod.POST,
                        new HttpEntity<>(request, headers),
                        String.class
                );

        // assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @DisplayName("GET /api/v1/products/{id}/reviews - 상품 리뷰 목록을 조회할 수 있다.")
    @Test
    void getProductReviews_success() {
        // arrange - 리뷰 작성
        HttpHeaders headers = TestAuthHelper.authJsonHeaders(accessToken);
        ReviewV1Dto.CreateReviewRequest request = new ReviewV1Dto.CreateReviewRequest(
                orderId, productId, 4, "괜찮은 상품입니다."
        );
        testRestTemplate.exchange(
                "/api/v1/reviews",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                String.class
        );

        // act (공개 API이므로 토큰 불필요)
        ResponseEntity<String> response =
                testRestTemplate.exchange(
                        "/api/v1/products/" + productId + "/reviews",
                        HttpMethod.GET,
                        null,
                        String.class
                );

        // assert
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody()).contains("\"averageRating\""),
                () -> assertThat(response.getBody()).contains("\"totalCount\":1"),
                () -> assertThat(response.getBody()).contains("괜찮은 상품입니다.")
        );
    }
}
