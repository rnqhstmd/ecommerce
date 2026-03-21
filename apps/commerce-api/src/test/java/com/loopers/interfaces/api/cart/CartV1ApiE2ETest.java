package com.loopers.interfaces.api.cart;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.cart.CartService;
import com.loopers.domain.point.PointService;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CartV1ApiE2ETest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PointService pointService;

    @Autowired
    private CartService cartService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String USER_ID = "cartuser";
    private Long productId;

    @BeforeEach
    void setUp() {
        // 사용자 생성
        UserV1Dto.RegisterRequest registerRequest = new UserV1Dto.RegisterRequest(
                USER_ID, "cart@example.com", "1990-01-01", Gender.MALE
        );
        testRestTemplate.postForEntity("/api/v1/users", registerRequest, ApiResponse.class);

        // 포인트 충전
        pointService.chargePoint(USER_ID, 1000000L);

        // 상품 생성
        Brand brand = brandRepository.save(Brand.create("Cart Brand"));
        Product product = productRepository.save(Product.create("Cart Product", 5000L, 100, brand.getId()));
        productId = product.getId();

        // 테스트 전 장바구니 초기화
        redisTemplate.delete("cart:" + USER_ID);
    }

    @AfterEach
    void tearDown() {
        redisTemplate.delete("cart:" + USER_ID);
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("POST /api/v1/cart/items - 장바구니 추가에 성공한다.")
    @Test
    void addItem_success() {
        // arrange
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-USER-ID", USER_ID);
        headers.setContentType(MediaType.APPLICATION_JSON);
        CartV1Dto.AddItemRequest request = new CartV1Dto.AddItemRequest(productId, 2);

        // act
        ResponseEntity<ApiResponse<CartV1Dto.AddItemResponse>> response =
                testRestTemplate.exchange(
                        "/api/v1/cart/items",
                        HttpMethod.POST,
                        new HttpEntity<>(request, headers),
                        new ParameterizedTypeReference<>() {}
                );

        // assert
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().data()).isNotNull(),
                () -> assertThat(response.getBody().data().productId()).isEqualTo(productId),
                () -> assertThat(response.getBody().data().quantity()).isEqualTo(2)
        );
    }

    @DisplayName("POST /api/v1/cart/items - X-USER-ID 없으면 401을 반환한다.")
    @Test
    void addItem_returnsUnauthorized_whenNoUserId() {
        // arrange
        CartV1Dto.AddItemRequest request = new CartV1Dto.AddItemRequest(productId, 1);

        // act
        ResponseEntity<ApiResponse<Object>> response =
                testRestTemplate.exchange(
                        "/api/v1/cart/items",
                        HttpMethod.POST,
                        new HttpEntity<>(request),
                        new ParameterizedTypeReference<>() {}
                );

        // assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @DisplayName("GET /api/v1/cart - 장바구니 조회 (상품 정보 포함)")
    @Test
    void getCart_success() {
        // arrange - 장바구니에 상품 추가
        cartService.addItem(USER_ID, productId, 3);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-USER-ID", USER_ID);

        // act
        ResponseEntity<String> response =
                testRestTemplate.exchange(
                        "/api/v1/cart",
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        String.class
                );

        // assert
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody()).contains("\"productName\""),
                () -> assertThat(response.getBody()).contains("\"price\""),
                () -> assertThat(response.getBody()).contains("\"stockStatus\""),
                () -> assertThat(response.getBody()).contains("Cart Product"),
                () -> assertThat(response.getBody()).contains("IN_STOCK")
        );
    }

    @DisplayName("GET /api/v1/cart - 빈 장바구니는 빈 배열을 반환한다.")
    @Test
    void getCart_returnsEmptyList_whenCartIsEmpty() {
        // arrange
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-USER-ID", USER_ID);

        // act
        ResponseEntity<String> response =
                testRestTemplate.exchange(
                        "/api/v1/cart",
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        String.class
                );

        // assert
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody()).contains("\"items\":[]")
        );
    }

    @DisplayName("DELETE /api/v1/cart/items/{productId} - 삭제에 성공한다.")
    @Test
    void removeItem_success() {
        // arrange - 장바구니에 상품 추가
        cartService.addItem(USER_ID, productId, 2);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-USER-ID", USER_ID);

        // act
        ResponseEntity<ApiResponse<Void>> response =
                testRestTemplate.exchange(
                        "/api/v1/cart/items/" + productId,
                        HttpMethod.DELETE,
                        new HttpEntity<>(headers),
                        new ParameterizedTypeReference<>() {}
                );

        // assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 삭제 후 장바구니가 비어 있는지 확인
        var items = cartService.getCartItems(USER_ID);
        assertThat(items).isEmpty();
    }

    @DisplayName("POST /api/v1/cart/checkout - checkout 성공 (주문 생성 + 장바구니 비워짐)")
    @Test
    void checkout_success() {
        // arrange - 장바구니에 상품 추가
        cartService.addItem(USER_ID, productId, 2);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-USER-ID", USER_ID);

        // act
        ResponseEntity<String> response =
                testRestTemplate.exchange(
                        "/api/v1/cart/checkout",
                        HttpMethod.POST,
                        new HttpEntity<>(headers),
                        String.class
                );

        // assert
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody()).contains("\"orderId\""),
                () -> assertThat(response.getBody()).contains("\"PAID\""),
                () -> assertThat(response.getBody()).contains("\"totalAmount\"")
        );

        // 장바구니가 비워졌는지 확인
        var remainingItems = cartService.getCartItems(USER_ID);
        assertThat(remainingItems).isEmpty();
    }

    @DisplayName("POST /api/v1/cart/checkout - 빈 장바구니 checkout 시 400을 반환한다.")
    @Test
    void checkout_returnsBadRequest_whenCartIsEmpty() {
        // arrange
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-USER-ID", USER_ID);

        // act
        ResponseEntity<ApiResponse<Object>> response =
                testRestTemplate.exchange(
                        "/api/v1/cart/checkout",
                        HttpMethod.POST,
                        new HttpEntity<>(headers),
                        new ParameterizedTypeReference<>() {}
                );

        // assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
