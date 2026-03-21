package com.loopers.interfaces.api.brand;

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
class BrandV1ApiE2ETest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("POST /api/v1/brands - 브랜드 생성에 성공한다.")
    @Test
    void createBrand_success() {
        // arrange
        BrandV1Dto.CreateRequest request = new BrandV1Dto.CreateRequest("New Brand");

        // act
        ParameterizedTypeReference<ApiResponse<BrandV1Dto.BrandResponse>> responseType =
                new ParameterizedTypeReference<>() {};
        ResponseEntity<ApiResponse<BrandV1Dto.BrandResponse>> response =
                testRestTemplate.exchange(
                        "/api/v1/brands",
                        HttpMethod.POST,
                        new HttpEntity<>(request),
                        responseType
                );

        // assert
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().data()).isNotNull(),
                () -> assertThat(response.getBody().data().name()).isEqualTo("New Brand"),
                () -> assertThat(response.getBody().data().id()).isNotNull()
        );
    }

    @DisplayName("POST /api/v1/brands - 브랜드명이 없으면 400을 반환한다.")
    @Test
    void createBrand_returnsBadRequest_whenNameIsBlank() {
        // arrange
        BrandV1Dto.CreateRequest request = new BrandV1Dto.CreateRequest("");

        // act
        ParameterizedTypeReference<ApiResponse<BrandV1Dto.BrandResponse>> responseType =
                new ParameterizedTypeReference<>() {};
        ResponseEntity<ApiResponse<BrandV1Dto.BrandResponse>> response =
                testRestTemplate.exchange(
                        "/api/v1/brands",
                        HttpMethod.POST,
                        new HttpEntity<>(request),
                        responseType
                );

        // assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
