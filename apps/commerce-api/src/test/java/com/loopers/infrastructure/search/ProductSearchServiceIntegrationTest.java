package com.loopers.infrastructure.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.loopers.application.product.ProductSearchResult;
import com.loopers.domain.product.ProductSearchPort;
import com.loopers.utils.DatabaseCleanUp;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
class ProductSearchServiceIntegrationTest {

    @Autowired
    private ProductSearchPort productSearchPort;

    @Autowired
    private ProductSearchRepository productSearchRepository;

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @BeforeEach
    void setUp() throws IOException {
        // 다른 테스트에서 ES 실패로 열린 Circuit Breaker 초기화
        circuitBreakerRegistry.getAllCircuitBreakers()
                .forEach(cb -> cb.reset());

        // products 인덱스가 없으면 커스텀 설정으로 생성
        boolean indexExists = elasticsearchClient.indices()
                .exists(e -> e.index("products")).value();
        if (!indexExists) {
            try (var is = new org.springframework.core.io.ClassPathResource(
                    "elasticsearch/products-index-settings.json").getInputStream()) {
                elasticsearchClient.indices().create(c -> c.index("products").withJson(is));
            }
        }

        // 기존 문서 삭제
        productSearchRepository.deleteAll();

        List<ProductDocument> documents = List.of(
                ProductDocument.builder()
                        .id(1L).name("나이키 운동화").brandId(10L).brandName("나이키")
                        .categoryId(1L).categoryName("신발").price(89000L).likeCount(5L).build(),
                ProductDocument.builder()
                        .id(2L).name("아디다스 런닝화").brandId(20L).brandName("아디다스")
                        .categoryId(1L).categoryName("신발").price(120000L).likeCount(3L).build(),
                ProductDocument.builder()
                        .id(3L).name("나이키 반팔 티셔츠").brandId(10L).brandName("나이키")
                        .categoryId(2L).categoryName("상의").price(45000L).likeCount(10L).build(),
                ProductDocument.builder()
                        .id(4L).name("뉴발란스 운동화").brandId(30L).brandName("뉴발란스")
                        .categoryId(1L).categoryName("신발").price(79000L).likeCount(7L).build()
        );
        productSearchRepository.saveAll(documents);

        // ES 인덱스 명시적 refresh
        elasticsearchClient.indices().refresh(r -> r.index("products"));

        // 인덱싱 확인
        long count = elasticsearchClient.count(c -> c.index("products")).count();
        if (count == 0) {
            throw new IllegalStateException("ES 인덱스에 문서가 0건입니다. 인덱싱 실패.");
        }
    }

    @AfterEach
    void tearDown() {
        productSearchRepository.deleteAll();
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("한글 형태소 검색 - '운동화를' 검색 시 '운동화' 포함 상품이 반환된다.")
    @Test
    void search_koreanMorpheme_returnsMatchingProducts() {
        // arrange & act — ProductSearchPort 직접 호출 (CircuitBreaker 우회)
        ProductSearchResult result = productSearchPort.searchProducts(
                "운동화를", null, null, null, 0, 10, Sort.unsorted()
        );

        // assert
        assertAll(
                () -> assertThat(result.productIds()).isNotEmpty(),
                () -> assertThat(result.productIds()).contains(1L, 4L),
                () -> assertThat(result.totalHits()).isGreaterThanOrEqualTo(2)
        );
    }

    @DisplayName("멀티필드 검색 - brandName으로 검색할 수 있다.")
    @Test
    void search_multiField_searchesByBrandName() {
        // arrange & act — ProductSearchPort 직접 호출 (CircuitBreaker 우회)
        ProductSearchResult result = productSearchPort.searchProducts(
                "아디다스", null, null, null, 0, 10, Sort.unsorted()
        );

        // assert
        assertAll(
                () -> assertThat(result.productIds()).contains(2L),
                () -> assertThat(result.totalHits()).isGreaterThanOrEqualTo(1)
        );
    }

    @DisplayName("필터링 - brandId, minPrice, maxPrice 조건으로 검색 결과를 필터링한다.")
    @Test
    void search_withFilters_returnsFilteredResults() {
        // arrange & act — ProductSearchPort 직접 호출 (CircuitBreaker 우회)
        ProductSearchResult result = productSearchPort.searchProducts(
                null, 10L, 40000L, 90000L, 0, 10, Sort.unsorted()
        );

        // assert
        assertAll(
                () -> assertThat(result.productIds()).contains(1L, 3L),
                () -> assertThat(result.productIds()).doesNotContain(2L),
                () -> assertThat(result.totalHits()).isEqualTo(2)
        );
    }

    @DisplayName("keyword가 없을 때 match_all로 전체 상품이 반환된다.")
    @Test
    void search_withoutKeyword_returnsAll() {
        // arrange & act — ProductSearchPort 직접 호출 (CircuitBreaker 우회)
        ProductSearchResult result = productSearchPort.searchProducts(
                null, null, null, null, 0, 10, Sort.unsorted()
        );

        // assert
        assertAll(
                () -> assertThat(result.productIds()).hasSize(4),
                () -> assertThat(result.totalHits()).isEqualTo(4)
        );
    }

    @DisplayName("자동완성 - 접두사 매칭으로 상품명이 반환된다.")
    @Test
    void autocomplete_returnsMatchingNames() {
        // arrange & act — ProductSearchPort 직접 호출 (CircuitBreaker 우회)
        List<String> suggestions = productSearchPort.autocomplete("나이키", 10);

        // assert
        assertAll(
                () -> assertThat(suggestions).isNotEmpty(),
                () -> assertThat(suggestions).allMatch(name -> name.contains("나이키"))
        );
    }
}
