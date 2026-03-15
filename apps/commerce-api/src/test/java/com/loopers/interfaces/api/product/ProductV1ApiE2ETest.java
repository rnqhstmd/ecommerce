package com.loopers.interfaces.api.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.interfaces.api.ApiResponse;
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
}
