package com.loopers.interfaces.api.like;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.Gender;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.user.UserV1Dto;
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
class LikeV1ApiE2ETest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private Long productId;

    @BeforeEach
    void setUp() {
        // 사용자 생성
        UserV1Dto.RegisterRequest registerRequest = new UserV1Dto.RegisterRequest(
                "likeuser01", "like@example.com", "1990-01-01", Gender.MALE
        );
        testRestTemplate.postForEntity("/api/v1/users", registerRequest, ApiResponse.class);

        // 상품 생성
        Brand brand = brandRepository.save(Brand.create("Like Brand"));
        Product product = productRepository.save(Product.create("Like Product", 1000L, 10, brand.getId()));
        productId = product.getId();
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("POST /api/v1/likes - 좋아요 등록에 성공한다.")
    @Test
    void addLike_success() {
        // arrange
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-USER-ID", "likeuser01");
        LikeV1Dto.LikeRequest request = new LikeV1Dto.LikeRequest(productId);

        // act
        ParameterizedTypeReference<ApiResponse<LikeV1Dto.LikeResponse>> responseType =
                new ParameterizedTypeReference<>() {};
        ResponseEntity<ApiResponse<LikeV1Dto.LikeResponse>> response =
                testRestTemplate.exchange(
                        "/api/v1/likes",
                        HttpMethod.POST,
                        new HttpEntity<>(request, headers),
                        responseType
                );

        // assert
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().userId()).isEqualTo("likeuser01"),
                () -> assertThat(response.getBody().data().productId()).isEqualTo(productId)
        );
    }

    @DisplayName("DELETE /api/v1/likes/{productId} - 좋아요 취소에 성공한다.")
    @Test
    void removeLike_success() {
        // arrange - 먼저 좋아요 등록
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-USER-ID", "likeuser01");
        LikeV1Dto.LikeRequest request = new LikeV1Dto.LikeRequest(productId);
        testRestTemplate.exchange(
                "/api/v1/likes",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                new ParameterizedTypeReference<ApiResponse<LikeV1Dto.LikeResponse>>() {}
        );

        // act
        ParameterizedTypeReference<ApiResponse<Void>> responseType =
                new ParameterizedTypeReference<>() {};
        ResponseEntity<ApiResponse<Void>> response =
                testRestTemplate.exchange(
                        "/api/v1/likes/" + productId,
                        HttpMethod.DELETE,
                        new HttpEntity<>(headers),
                        responseType
                );

        // assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @DisplayName("POST /api/v1/likes - X-USER-ID 없으면 401을 반환한다.")
    @Test
    void addLike_returnsUnauthorized_whenNoUserId() {
        // arrange
        LikeV1Dto.LikeRequest request = new LikeV1Dto.LikeRequest(productId);

        // act
        ParameterizedTypeReference<ApiResponse<LikeV1Dto.LikeResponse>> responseType =
                new ParameterizedTypeReference<>() {};
        ResponseEntity<ApiResponse<LikeV1Dto.LikeResponse>> response =
                testRestTemplate.exchange(
                        "/api/v1/likes",
                        HttpMethod.POST,
                        new HttpEntity<>(request),
                        responseType
                );

        // assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
