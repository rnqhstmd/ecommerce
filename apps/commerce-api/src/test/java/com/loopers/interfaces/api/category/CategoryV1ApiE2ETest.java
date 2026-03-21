package com.loopers.interfaces.api.category;

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
class CategoryV1ApiE2ETest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("POST /api/v1/categories - 카테고리 생성에 성공한다.")
    @Test
    void createCategory_success() {
        // arrange
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        CategoryV1Dto.CreateRequest request = new CategoryV1Dto.CreateRequest("의류", null);

        // act
        ResponseEntity<String> response =
                testRestTemplate.exchange(
                        "/api/v1/categories",
                        HttpMethod.POST,
                        new HttpEntity<>(request, headers),
                        String.class
                );

        // assert
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody()).contains("\"categoryId\""),
                () -> assertThat(response.getBody()).contains("\"name\":\"의류\""),
                () -> assertThat(response.getBody()).contains("\"depth\":0")
        );
    }

    @DisplayName("GET /api/v1/categories - 카테고리 목록을 조회할 수 있다.")
    @Test
    void getCategories_success() {
        // arrange - 카테고리 생성
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        CategoryV1Dto.CreateRequest rootRequest = new CategoryV1Dto.CreateRequest("전자제품", null);
        testRestTemplate.exchange(
                "/api/v1/categories",
                HttpMethod.POST,
                new HttpEntity<>(rootRequest, headers),
                String.class
        );

        CategoryV1Dto.CreateRequest secondRequest = new CategoryV1Dto.CreateRequest("식품", null);
        testRestTemplate.exchange(
                "/api/v1/categories",
                HttpMethod.POST,
                new HttpEntity<>(secondRequest, headers),
                String.class
        );

        // act
        ResponseEntity<String> response =
                testRestTemplate.exchange(
                        "/api/v1/categories",
                        HttpMethod.GET,
                        null,
                        String.class
                );

        // assert
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody()).contains("전자제품"),
                () -> assertThat(response.getBody()).contains("식품")
        );
    }
}
