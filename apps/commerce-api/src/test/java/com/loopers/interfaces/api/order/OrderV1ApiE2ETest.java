package com.loopers.interfaces.api.order;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
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
import org.springframework.http.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderV1ApiE2ETest {

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

    private Long productId;

    @BeforeEach
    void setUp() {
        // 사용자 생성
        UserV1Dto.RegisterRequest registerRequest = new UserV1Dto.RegisterRequest(
                "orderuser", "order@example.com", "1990-01-01", Gender.MALE
        );
        testRestTemplate.postForEntity("/api/v1/users", registerRequest, ApiResponse.class);

        // 포인트 충전
        pointService.chargePoint("orderuser", 100000L);

        // 상품 생성
        Brand brand = brandRepository.save(Brand.create("Order Brand"));
        Product product = productRepository.save(Product.create("Order Product", 1000L, 10, brand.getId()));
        productId = product.getId();
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("POST /api/v1/orders - 주문 생성에 성공한다.")
    @Test
    void placeOrder_success() {
        // arrange
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-USER-ID", "orderuser");
        OrderV1Dto.PlaceOrderRequest request = new OrderV1Dto.PlaceOrderRequest(
                List.of(new OrderV1Dto.OrderItemRequest(productId, 2))
        );

        // act
        ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>> responseType =
                new ParameterizedTypeReference<>() {};
        ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> response =
                testRestTemplate.exchange(
                        "/api/v1/orders",
                        HttpMethod.POST,
                        new HttpEntity<>(request, headers),
                        responseType
                );

        // assert
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().data()).isNotNull(),
                () -> assertThat(response.getBody().data().userId()).isEqualTo("orderuser"),
                () -> assertThat(response.getBody().data().totalAmount()).isEqualTo(2000L),
                () -> assertThat(response.getBody().data().status()).isEqualTo("PAID"),
                () -> assertThat(response.getBody().data().items()).hasSize(1)
        );
    }

    @DisplayName("POST /api/v1/orders - X-USER-ID 없으면 401을 반환한다.")
    @Test
    void placeOrder_returnsUnauthorized_whenNoUserId() {
        // arrange
        OrderV1Dto.PlaceOrderRequest request = new OrderV1Dto.PlaceOrderRequest(
                List.of(new OrderV1Dto.OrderItemRequest(productId, 1))
        );

        // act
        ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>> responseType =
                new ParameterizedTypeReference<>() {};
        ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> response =
                testRestTemplate.exchange(
                        "/api/v1/orders",
                        HttpMethod.POST,
                        new HttpEntity<>(request),
                        responseType
                );

        // assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @DisplayName("GET /api/v1/orders - X-USER-ID 없으면 401을 반환한다.")
    @Test
    void getMyOrders_returnsUnauthorized_whenNoUserId() {
        // act
        ResponseEntity<String> response =
                testRestTemplate.exchange(
                        "/api/v1/orders",
                        HttpMethod.GET,
                        null,
                        String.class
                );

        // assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @DisplayName("GET /api/v1/orders - 주문이 없으면 빈 목록을 반환한다.")
    @Test
    void getMyOrders_returnsEmptyList() {
        // arrange
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-USER-ID", "orderuser");

        // act
        ResponseEntity<String> response =
                testRestTemplate.exchange(
                        "/api/v1/orders",
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        String.class
                );

        // assert
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody()).contains("\"content\":[]"),
                () -> assertThat(response.getBody()).contains("\"totalElements\":0")
        );
    }

    @DisplayName("GET /api/v1/orders - 주문 후 목록에 포함된다.")
    @Test
    void getMyOrders_returnsOrderList() {
        // arrange - 주문 생성
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-USER-ID", "orderuser");
        OrderV1Dto.PlaceOrderRequest request = new OrderV1Dto.PlaceOrderRequest(
                List.of(new OrderV1Dto.OrderItemRequest(productId, 1))
        );
        testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                new ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderResponse>>() {}
        );

        // act
        ResponseEntity<String> response =
                testRestTemplate.exchange(
                        "/api/v1/orders",
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        String.class
                );

        // assert
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody()).contains("\"totalElements\":1"),
                () -> assertThat(response.getBody()).contains("\"PAID\"")
        );
    }

    @DisplayName("GET /api/v1/orders/{id} - X-USER-ID 없으면 401을 반환한다.")
    @Test
    void getOrderDetail_returnsUnauthorized_whenNoUserId() {
        // act
        ResponseEntity<ApiResponse<Object>> response =
                testRestTemplate.exchange(
                        "/api/v1/orders/1",
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<>() {}
                );

        // assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @DisplayName("GET /api/v1/orders/{id} - 존재하지 않는 주문 조회 시 404를 반환한다.")
    @Test
    void getOrderDetail_returnsNotFound_whenOrderNotExists() {
        // arrange
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-USER-ID", "orderuser");

        // act
        ResponseEntity<ApiResponse<Object>> response =
                testRestTemplate.exchange(
                        "/api/v1/orders/99999",
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        new ParameterizedTypeReference<>() {}
                );

        // assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @DisplayName("GET /api/v1/orders/{id} - 주문 상세 조회에 성공한다.")
    @Test
    void getOrderDetail_success() {
        // arrange - 주문 생성
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-USER-ID", "orderuser");
        OrderV1Dto.PlaceOrderRequest request = new OrderV1Dto.PlaceOrderRequest(
                List.of(new OrderV1Dto.OrderItemRequest(productId, 2))
        );
        ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> createResponse =
                testRestTemplate.exchange(
                        "/api/v1/orders",
                        HttpMethod.POST,
                        new HttpEntity<>(request, headers),
                        new ParameterizedTypeReference<>() {}
                );
        Long orderId = createResponse.getBody().data().orderId();

        // act
        ResponseEntity<ApiResponse<OrderV1Dto.OrderResponse>> response =
                testRestTemplate.exchange(
                        "/api/v1/orders/" + orderId,
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        new ParameterizedTypeReference<>() {}
                );

        // assert
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().data().orderId()).isEqualTo(orderId),
                () -> assertThat(response.getBody().data().userId()).isEqualTo("orderuser"),
                () -> assertThat(response.getBody().data().totalAmount()).isEqualTo(2000L),
                () -> assertThat(response.getBody().data().items()).hasSize(1)
        );
    }
}
