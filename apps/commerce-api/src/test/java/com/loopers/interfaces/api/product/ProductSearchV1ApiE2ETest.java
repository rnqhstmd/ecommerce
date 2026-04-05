package com.loopers.interfaces.api.product;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.infrastructure.search.ProductDocument;
import com.loopers.infrastructure.search.ProductSearchRepository;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.ArrayList;
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
    private ElasticsearchClient elasticsearchClient;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @BeforeEach
    void setUp() throws IOException {
        productSearchRepository.deleteAll();
        databaseCleanUp.truncateAllTables();

        // 1) MySQL 브랜드 저장
        Brand nikeBrand = brandRepository.save(Brand.create("나이키"));
        Brand adidasBrand = brandRepository.save(Brand.create("아디다스"));
        Brand nikeEnBrand = brandRepository.save(Brand.create("Nike"));

        // 2) MySQL 상품 저장 — 저장 후 반환된 id로 ES 문서 id를 일치시켜야 ProductFacade의 MySQL 상세 조회가 동작
        Product p1 = productRepository.save(Product.create("나이키 에어맥스", 159000L, 100, nikeBrand.getId()));
        Product p2 = productRepository.save(Product.create("나이키 덩크 로우", 129000L, 100, nikeBrand.getId()));
        Product p3 = productRepository.save(Product.create("아디다스 슈퍼스타", 109000L, 100, adidasBrand.getId()));
        Product p4 = productRepository.save(Product.create("나이키 테크 플리스", 89000L, 100, nikeBrand.getId()));
        // AC-3 검증용: 영문 상품 (Nike <-> Nile 편집거리 1)
        Product p5 = productRepository.save(Product.create("Nike Air Force 1", 149000L, 100, nikeEnBrand.getId()));
        // AC-2 검증용: 상품명에 브랜드명 '아디다스'가 없고 brandName만 매칭되는 케이스
        Product p6 = productRepository.save(Product.create("런닝 프로 맥스", 99000L, 100, adidasBrand.getId()));

        // 3) ES 문서 — id를 MySQL Product.id에 정확히 맞춤
        List<ProductDocument> documents = new ArrayList<>();
        documents.add(ProductDocument.builder()
                .id(p1.getId()).name("나이키 에어맥스").brandId(nikeBrand.getId()).brandName("나이키")
                .categoryId(1L).categoryName("신발").price(159000L).likeCount(20L).build());
        documents.add(ProductDocument.builder()
                .id(p2.getId()).name("나이키 덩크 로우").brandId(nikeBrand.getId()).brandName("나이키")
                .categoryId(1L).categoryName("신발").price(129000L).likeCount(15L).build());
        documents.add(ProductDocument.builder()
                .id(p3.getId()).name("아디다스 슈퍼스타").brandId(adidasBrand.getId()).brandName("아디다스")
                .categoryId(1L).categoryName("신발").price(109000L).likeCount(10L).build());
        documents.add(ProductDocument.builder()
                .id(p4.getId()).name("나이키 테크 플리스").brandId(nikeBrand.getId()).brandName("나이키")
                .categoryId(2L).categoryName("상의").price(89000L).likeCount(8L).build());
        documents.add(ProductDocument.builder()
                .id(p5.getId()).name("Nike Air Force 1").brandId(nikeEnBrand.getId()).brandName("Nike")
                .categoryId(1L).categoryName("신발").price(149000L).likeCount(12L).build());
        documents.add(ProductDocument.builder()
                .id(p6.getId()).name("런닝 프로 맥스").brandId(adidasBrand.getId()).brandName("아디다스")
                .categoryId(1L).categoryName("신발").price(99000L).likeCount(8L).build());
        productSearchRepository.saveAll(documents);

        // 4) ES 인덱스 명시적 refresh (Thread.sleep 제거)
        elasticsearchClient.indices().refresh(r -> r.index("products"));
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

    @Nested
    @DisplayName("GET /api/v1/products — 하이라이트/Fuzzy 통합")
    class GetProducts {

        @DisplayName("keyword=나이키 - contents[].highlight.name에 <em>나이키</em> 조각이 존재한다.")
        @Test
        void getProducts_highlightName_whenKeywordMatchesName() {
            // act
            ResponseEntity<ApiResponse<ProductV1Dto.ProductListResponse>> response =
                    testRestTemplate.exchange(
                            "/api/v1/products?keyword=나이키",
                            HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<>() {}
                    );

            // assert
            assertAll(
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data()).isNotNull(),
                    () -> assertThat(response.getBody().data().contents()).isNotEmpty(),
                    () -> assertThat(response.getBody().data().contents())
                            .anySatisfy(content -> {
                                assertThat(content.highlight()).isNotNull();
                                assertThat(content.highlight()).containsKey("name");
                                assertThat(content.highlight().get("name"))
                                        .anyMatch(fragment -> fragment.contains("<em>")
                                                && fragment.contains("</em>"));
                            })
            );
        }

        @DisplayName("keyword=아디다스 - brandName 키만 포함되고 name/categoryName 키는 생략된다.")
        @Test
        void getProducts_highlightBrandOnly_whenKeywordMatchesBrandName() {
            // act
            ResponseEntity<ApiResponse<ProductV1Dto.ProductListResponse>> response =
                    testRestTemplate.exchange(
                            "/api/v1/products?keyword=아디다스",
                            HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<>() {}
                    );

            // assert
            assertAll(
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data().contents()).isNotEmpty(),
                    () -> assertThat(response.getBody().data().contents())
                            .anySatisfy(content -> {
                                assertThat(content.highlight()).containsKey("brandName");
                                assertThat(content.highlight()).doesNotContainKey("name");
                                assertThat(content.highlight()).doesNotContainKey("categoryName");
                                // Nori가 '아디다스'를 '아디/다스'로 분해하므로 fragment는
                                // '<em>아디</em><em>다스</em>' 형태가 될 수 있다. <em> 태그를 제거한
                                // 원문이 '아디다스'를 포함하고, <em> 태그가 실제로 감싸져 있음을 검증.
                                assertThat(content.highlight().get("brandName"))
                                        .anyMatch(fragment -> fragment.contains("<em>")
                                                && fragment.contains("</em>")
                                                && fragment.replace("<em>", "").replace("</em>", "").contains("아디다스"));
                            })
            );
        }

        @DisplayName("keyword=Nile - 한 글자 오타(Nike)여도 totalElements > 0 이다 (Fuzzy AUTO).")
        @Test
        void getProducts_fuzziness_returnsResultsForTypo() {
            // act — 한글 3자 '나이크'는 AUTO 정책상 fuzzy 비활성이므로
            // 영문 4자 'Nile'(Nike와 편집거리 1)로 AC-3 오타 관용을 검증한다.
            ResponseEntity<ApiResponse<ProductV1Dto.ProductListResponse>> response =
                    testRestTemplate.exchange(
                            "/api/v1/products?keyword=Nile",
                            HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<>() {}
                    );

            // assert
            assertAll(
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data().totalElements()).isGreaterThan(0)
            );
        }

        @DisplayName("keyword 미지정 - 전체 조회 시 모든 contents[]의 highlight는 빈 맵이다.")
        @Test
        void getProducts_noKeyword_allHighlightsAreEmpty() {
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
                    () -> assertThat(response.getBody().data().contents()).isNotEmpty(),
                    () -> assertThat(response.getBody().data().contents())
                            .allSatisfy(content -> {
                                assertThat(content.highlight()).isNotNull();
                                assertThat(content.highlight()).isEmpty();
                            })
            );
        }

        @DisplayName("keyword=ㅁㅁㅁ - 무관한 키워드는 totalElements가 0이다.")
        @Test
        void getProducts_unrelatedKeyword_returnsZeroResults() {
            // act
            ResponseEntity<ApiResponse<ProductV1Dto.ProductListResponse>> response =
                    testRestTemplate.exchange(
                            "/api/v1/products?keyword=ㅁㅁㅁ",
                            HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<>() {}
                    );

            // assert
            assertAll(
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data().totalElements()).isEqualTo(0),
                    () -> assertThat(response.getBody().data().contents()).isEmpty()
            );
        }
    }
}
