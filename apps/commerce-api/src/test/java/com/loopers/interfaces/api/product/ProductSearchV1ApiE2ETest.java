package com.loopers.interfaces.api.product;

import com.loopers.infrastructure.search.ProductDocument;
import com.loopers.infrastructure.search.ProductSearchRepository;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductSearchV1ApiE2ETest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private ProductSearchRepository productSearchRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @BeforeEach
    void setUp() {
        productSearchRepository.deleteAll();

        List<ProductDocument> documents = List.of(
                ProductDocument.builder()
                        .id(1L).name("나이키 에어맥스").brandId(10L).brandName("나이키")
                        .categoryId(1L).categoryName("신발").price(159000L).likeCount(20L).build(),
                ProductDocument.builder()
                        .id(2L).name("나이키 덩크 로우").brandId(10L).brandName("나이키")
                        .categoryId(1L).categoryName("신발").price(129000L).likeCount(15L).build(),
                ProductDocument.builder()
                        .id(3L).name("아디다스 슈퍼스타").brandId(20L).brandName("아디다스")
                        .categoryId(1L).categoryName("신발").price(109000L).likeCount(10L).build(),
                ProductDocument.builder()
                        .id(4L).name("나이키 테크 플리스").brandId(10L).brandName("나이키")
                        .categoryId(2L).categoryName("상의").price(89000L).likeCount(8L).build()
        );
        productSearchRepository.saveAll(documents);

        // ES 인덱스 refresh 대기
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
    }

    @AfterEach
    void tearDown() {
        productSearchRepository.deleteAll();
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("GET /api/v1/products/search/autocomplete?keyword=나이 - suggestions를 반환한다.")
    @Test
    void autocomplete_returnsSuggestions() {
        // act
        ResponseEntity<ApiResponse<ProductSearchV1Dto.AutocompleteResponse>> response =
                testRestTemplate.exchange(
                        "/api/v1/products/search/autocomplete?keyword=나이",
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<>() {}
                );

        // assert
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().data()).isNotNull(),
                () -> assertThat(response.getBody().data().suggestions()).isNotEmpty(),
                () -> assertThat(response.getBody().data().suggestions())
                        .allMatch(s -> s.contains("나이키"))
        );
    }

    @DisplayName("GET /api/v1/products/search/autocomplete?keyword= - 빈 keyword는 빈 배열을 반환한다.")
    @Test
    void autocomplete_returnsEmptyList_whenKeywordIsBlank() {
        // act
        ResponseEntity<ApiResponse<ProductSearchV1Dto.AutocompleteResponse>> response =
                testRestTemplate.exchange(
                        "/api/v1/products/search/autocomplete?keyword=",
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<>() {}
                );

        // assert
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().data().suggestions()).isEmpty()
        );
    }

    @DisplayName("GET /api/v1/products/search/autocomplete?size=21 - size 초과 시 400을 반환한다.")
    @Test
    void autocomplete_returnsBadRequest_whenSizeExceeds20() {
        // act
        ResponseEntity<ApiResponse<Object>> response =
                testRestTemplate.exchange(
                        "/api/v1/products/search/autocomplete?keyword=나이키&size=21",
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<>() {}
                );

        // assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @DisplayName("GET /api/v1/products/search/facets - brandFacets, categoryFacets, priceRanges를 반환한다.")
    @Test
    void facets_returnsFacetAggregations() {
        // act
        ResponseEntity<ApiResponse<ProductSearchV1Dto.FacetResponse>> response =
                testRestTemplate.exchange(
                        "/api/v1/products/search/facets",
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<>() {}
                );

        // assert
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().data()).isNotNull(),
                () -> assertThat(response.getBody().data().brandFacets()).isNotEmpty(),
                () -> assertThat(response.getBody().data().categoryFacets()).isNotEmpty(),
                () -> assertThat(response.getBody().data().priceRanges()).isNotEmpty()
        );
    }

    @DisplayName("GET /api/v1/products/search/facets?keyword=나이키 - 필터링된 집계 결과를 반환한다.")
    @Test
    void facets_returnsFilteredAggregations_whenKeywordProvided() {
        // act
        ResponseEntity<ApiResponse<ProductSearchV1Dto.FacetResponse>> response =
                testRestTemplate.exchange(
                        "/api/v1/products/search/facets?keyword=나이키",
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<>() {}
                );

        // assert
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().data()).isNotNull(),
                () -> assertThat(response.getBody().data().brandFacets()).isNotEmpty(),
                () -> assertThat(response.getBody().data().brandFacets())
                        .anyMatch(b -> b.name().equals("나이키"))
        );
    }
}
