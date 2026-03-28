package com.loopers.interfaces.api.auth;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.product.ProductV1Dto;
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
class RbacE2ETest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        adminToken = TestAuthHelper.signupAndGetAdminToken(
                testRestTemplate, jwtTokenProvider,
                "rbacadmin", "rbacadmin@example.com", "password123"
        );
        userToken = TestAuthHelper.signupAndGetToken(
                testRestTemplate, "rbacuser", "rbacuser@example.com", "password123"
        );
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("ADMIN 토큰으로 POST /api/v1/products 요청 시 상품 생성에 성공한다.")
    @Test
    void createProduct_success_withAdminToken() {
        // arrange
        Brand brand = brandRepository.save(Brand.create("RBAC Brand"));
        ProductV1Dto.CreateRequest request = new ProductV1Dto.CreateRequest(
                "Admin Product", 10000L, 100, brand.getId()
        );
        HttpHeaders headers = TestAuthHelper.authHeaders(adminToken);

        // act
        ResponseEntity<ApiResponse<ProductV1Dto.ProductResponse>> response =
                testRestTemplate.exchange(
                        "/api/v1/products",
                        HttpMethod.POST,
                        new HttpEntity<>(request, headers),
                        new ParameterizedTypeReference<>() {}
                );

        // assert
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().data().productName()).isEqualTo("Admin Product")
        );
    }

    @DisplayName("USER 토큰으로 POST /api/v1/products 요청 시 403을 반환한다.")
    @Test
    void createProduct_returnsForbidden_withUserToken() {
        // arrange
        Brand brand = brandRepository.save(Brand.create("RBAC Brand"));
        ProductV1Dto.CreateRequest request = new ProductV1Dto.CreateRequest(
                "User Product", 10000L, 100, brand.getId()
        );
        HttpHeaders headers = TestAuthHelper.authHeaders(userToken);

        // act
        ResponseEntity<ApiResponse<Object>> response =
                testRestTemplate.exchange(
                        "/api/v1/products",
                        HttpMethod.POST,
                        new HttpEntity<>(request, headers),
                        new ParameterizedTypeReference<>() {}
                );

        // assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
