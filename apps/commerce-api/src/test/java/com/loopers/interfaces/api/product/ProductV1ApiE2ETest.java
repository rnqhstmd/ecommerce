package com.loopers.interfaces.api.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.like.LikeService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.Gender;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.like.LikeV1Dto;
import com.loopers.interfaces.api.user.UserV1Dto;
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
class ProductV1ApiE2ETest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("POST /api/v1/products - 상품 생성에 성공한다.")
    @Test
    void createProduct_success() {
        // arrange
        Brand brand = brandRepository.save(Brand.create("Test Brand"));
        ProductV1Dto.CreateRequest request = new ProductV1Dto.CreateRequest(
                "Test Product", 10000L, 100, brand.getId()
        );

        // act
        ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductResponse>> responseType =
                new ParameterizedTypeReference<>() {};
        ResponseEntity<ApiResponse<ProductV1Dto.ProductResponse>> response =
                testRestTemplate.exchange(
                        "/api/v1/products",
                        HttpMethod.POST,
                        new HttpEntity<>(request),
                        responseType
                );

        // assert
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().data()).isNotNull(),
                () -> assertThat(response.getBody().data().productName()).isEqualTo("Test Product"),
                () -> assertThat(response.getBody().data().price()).isEqualTo(10000L),
                () -> assertThat(response.getBody().data().stock()).isEqualTo(100),
                () -> assertThat(response.getBody().data().brandId()).isEqualTo(brand.getId())
        );
    }

    @DisplayName("GET /api/v1/products/{id} - 상품 상세 조회에 성공한다.")
    @Test
    void getProduct_success() {
        // arrange
        Brand brand = brandRepository.save(Brand.create("Test Brand"));
        ProductV1Dto.CreateRequest createRequest = new ProductV1Dto.CreateRequest(
                "Detail Product", 5000L, 50, brand.getId()
        );
        ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductResponse>> createType =
                new ParameterizedTypeReference<>() {};
        ResponseEntity<ApiResponse<ProductV1Dto.ProductResponse>> createResponse =
                testRestTemplate.exchange(
                        "/api/v1/products",
                        HttpMethod.POST,
                        new HttpEntity<>(createRequest),
                        createType
                );
        Long productId = createResponse.getBody().data().productId();

        // act
        ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductResponse>> responseType =
                new ParameterizedTypeReference<>() {};
        ResponseEntity<ApiResponse<ProductV1Dto.ProductResponse>> response =
                testRestTemplate.exchange(
                        "/api/v1/products/" + productId,
                        HttpMethod.GET,
                        null,
                        responseType
                );

        // assert
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().productName()).isEqualTo("Detail Product")
        );
    }

    @DisplayName("GET /api/v1/products - 기본 조회 (페이징 검증)")
    @Test
    void getProducts_defaultPaging() {
        // arrange
        Brand brand = brandRepository.save(Brand.create("Paging Brand"));
        for (int i = 1; i <= 3; i++) {
            productRepository.save(Product.create("Product " + i, 1000L * i, 10, brand.getId()));
        }

        // act
        ResponseEntity<ApiResponse<ProductV1Dto.ProductListResponse>> response =
                testRestTemplate.exchange(
                        "/api/v1/products",
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<>() {}
                );

        // assert
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().data().contents()).hasSize(3),
                () -> assertThat(response.getBody().data().page()).isEqualTo(0),
                () -> assertThat(response.getBody().data().totalElements()).isEqualTo(3)
        );
    }

    @DisplayName("GET /api/v1/products?brandId={id} - 브랜드 필터")
    @Test
    void getProducts_filterByBrand() {
        // arrange
        Brand brand1 = brandRepository.save(Brand.create("Brand A"));
        Brand brand2 = brandRepository.save(Brand.create("Brand B"));
        productRepository.save(Product.create("Product A1", 1000L, 10, brand1.getId()));
        productRepository.save(Product.create("Product A2", 2000L, 10, brand1.getId()));
        productRepository.save(Product.create("Product B1", 3000L, 10, brand2.getId()));

        // act
        ResponseEntity<ApiResponse<ProductV1Dto.ProductListResponse>> response =
                testRestTemplate.exchange(
                        "/api/v1/products?brandId=" + brand1.getId(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<>() {}
                );

        // assert
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().contents()).hasSize(2),
                () -> assertThat(response.getBody().data().contents())
                        .allMatch(c -> c.brandId().equals(brand1.getId()))
        );
    }

    @DisplayName("GET /api/v1/products?sort=price_asc - 가격 오름차순 정렬")
    @Test
    void getProducts_sortByPriceAsc() {
        // arrange
        Brand brand = brandRepository.save(Brand.create("Sort Brand"));
        productRepository.save(Product.create("Expensive", 5000L, 10, brand.getId()));
        productRepository.save(Product.create("Cheap", 1000L, 10, brand.getId()));
        productRepository.save(Product.create("Mid", 3000L, 10, brand.getId()));

        // act
        ResponseEntity<ApiResponse<ProductV1Dto.ProductListResponse>> response =
                testRestTemplate.exchange(
                        "/api/v1/products?sort=price_asc",
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<>() {}
                );

        // assert
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().contents().get(0).price()).isEqualTo(1000L),
                () -> assertThat(response.getBody().data().contents().get(1).price()).isEqualTo(3000L),
                () -> assertThat(response.getBody().data().contents().get(2).price()).isEqualTo(5000L)
        );
    }

    @DisplayName("GET /api/v1/products?sort=invalid - 잘못된 정렬 파라미터는 400을 반환한다.")
    @Test
    void getProducts_invalidSort_returnsBadRequest() {
        // act
        ResponseEntity<ApiResponse<Object>> response =
                testRestTemplate.exchange(
                        "/api/v1/products?sort=invalid",
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<>() {}
                );

        // assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @DisplayName("GET /api/v1/products?page=-1 - 음수 페이지는 400을 반환한다.")
    @Test
    void getProducts_negativePage_returnsBadRequest() {
        // act
        ResponseEntity<ApiResponse<Object>> response =
                testRestTemplate.exchange(
                        "/api/v1/products?page=-1",
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<>() {}
                );

        // assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @DisplayName("GET /api/v1/products?size=0 - size 0은 400을 반환한다.")
    @Test
    void getProducts_zeroSize_returnsBadRequest() {
        // act
        ResponseEntity<ApiResponse<Object>> response =
                testRestTemplate.exchange(
                        "/api/v1/products?size=0",
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<>() {}
                );

        // assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @DisplayName("GET /api/v1/products?size=101 - size 101은 400을 반환한다.")
    @Test
    void getProducts_oversizedSize_returnsBadRequest() {
        // act
        ResponseEntity<ApiResponse<Object>> response =
                testRestTemplate.exchange(
                        "/api/v1/products?size=101",
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<>() {}
                );

        // assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @DisplayName("GET /api/v1/products/{id} - 비로그인 시 isLiked는 null이다.")
    @Test
    void getProduct_isLikedNull_whenNotLoggedIn() {
        // arrange
        Brand brand = brandRepository.save(Brand.create("Test Brand"));
        Product product = productRepository.save(Product.create("LikeTest Product", 1000L, 10, brand.getId()));

        // act
        ResponseEntity<ApiResponse<ProductV1Dto.ProductResponse>> response =
                testRestTemplate.exchange(
                        "/api/v1/products/" + product.getId(),
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<>() {}
                );

        // assert
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().isLiked()).isNull()
        );
    }

    @DisplayName("GET /api/v1/products/{id} + X-USER-ID - 좋아요 여부가 반영된다.")
    @Test
    void getProduct_isLikedReflected_whenLoggedIn() {
        // arrange - 사용자 생성
        UserV1Dto.RegisterRequest registerRequest = new UserV1Dto.RegisterRequest(
                "productlikeuser", "plike@example.com", "1990-01-01", Gender.MALE
        );
        testRestTemplate.postForEntity("/api/v1/users", registerRequest, ApiResponse.class);

        Brand brand = brandRepository.save(Brand.create("Like Brand"));
        Product product1 = productRepository.save(Product.create("Liked Product", 1000L, 10, brand.getId()));
        Product product2 = productRepository.save(Product.create("Not Liked Product", 2000L, 10, brand.getId()));

        // 좋아요 등록
        HttpHeaders likeHeaders = new HttpHeaders();
        likeHeaders.set("X-USER-ID", "productlikeuser");
        likeHeaders.setContentType(MediaType.APPLICATION_JSON);
        LikeV1Dto.LikeRequest likeRequest = new LikeV1Dto.LikeRequest(product1.getId());
        testRestTemplate.exchange(
                "/api/v1/likes",
                HttpMethod.POST,
                new HttpEntity<>(likeRequest, likeHeaders),
                new ParameterizedTypeReference<ApiResponse<LikeV1Dto.LikeResponse>>() {}
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-USER-ID", "productlikeuser");

        // act - 좋아요한 상품 조회
        ResponseEntity<ApiResponse<ProductV1Dto.ProductResponse>> likedResponse =
                testRestTemplate.exchange(
                        "/api/v1/products/" + product1.getId(),
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        new ParameterizedTypeReference<>() {}
                );

        // act - 좋아요하지 않은 상품 조회
        ResponseEntity<ApiResponse<ProductV1Dto.ProductResponse>> notLikedResponse =
                testRestTemplate.exchange(
                        "/api/v1/products/" + product2.getId(),
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        new ParameterizedTypeReference<>() {}
                );

        // assert
        assertAll(
                () -> assertThat(likedResponse.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(likedResponse.getBody().data().isLiked()).isTrue(),
                () -> assertThat(notLikedResponse.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(notLikedResponse.getBody().data().isLiked()).isFalse()
        );
    }
}
