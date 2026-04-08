package com.loopers.infrastructure.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.loopers.domain.product.ProductSearchHit;
import com.loopers.domain.product.ProductSearchResult;
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
                        .categoryId(1L).categoryName("신발").price(79000L).likeCount(7L).build(),
                // AC-3 검증용: 영문 상품 (Nike <-> Nile 편집거리 1, AUTO 4자 이상이므로 fuzzy 유효)
                ProductDocument.builder()
                        .id(5L).name("Nike Air Force 1").brandId(40L).brandName("Nike")
                        .categoryId(1L).categoryName("신발").price(149000L).likeCount(12L).build(),
                // AC-2 검증용: 상품명에는 브랜드명이 없고 brandName만 '아디다스'
                ProductDocument.builder()
                        .id(6L).name("런닝 프로 맥스").brandId(20L).brandName("아디다스")
                        .categoryId(1L).categoryName("신발").price(99000L).likeCount(8L).build()
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
                () -> assertThat(result.productIds()).hasSize(6),
                () -> assertThat(result.totalHits()).isEqualTo(6)
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

    @DisplayName("하이라이트 - '나이키' 검색 시 name 필드에 <em>나이키</em> 조각이 포함된다.")
    @Test
    void search_highlight_exactNameMatch_wrapsKeywordWithEmTag() {
        // arrange & act
        ProductSearchResult result = productSearchPort.searchProducts(
                "나이키", null, null, null, 0, 10, Sort.unsorted()
        );

        // assert
        assertAll(
                () -> assertThat(result.hits()).isNotEmpty(),
                () -> assertThat(result.hits())
                        .anySatisfy(hit -> {
                            assertThat(hit.highlights()).containsKey("name");
                            assertThat(hit.highlights().get("name"))
                                    .anyMatch(fragment -> fragment.contains("<em>") && fragment.contains("</em>"));
                        })
        );
    }

    @DisplayName("하이라이트 - '아디다스' 검색 시 brandName 키만 포함되고 name/categoryName 키는 생략된다.")
    @Test
    void search_highlight_brandOnlyMatch_omitsOtherFieldKeys() {
        // arrange & act
        ProductSearchResult result = productSearchPort.searchProducts(
                "아디다스", null, null, null, 0, 10, Sort.unsorted()
        );

        // assert — 6L(상품명: "런닝 프로 맥스")은 brandName만 '아디다스'에 매칭된다.
        // Nori가 '아디다스'를 '아디/다스'로 분해해도 상품명에 두 토큰이 없어 name 키는 생성되지 않는다.
        assertAll(
                () -> assertThat(result.hits()).isNotEmpty(),
                () -> {
                    ProductSearchHit hit = result.hits().stream()
                            .filter(h -> h.productId().equals(6L))
                            .findFirst()
                            .orElseThrow();
                    assertAll(
                            () -> assertThat(hit.highlights()).containsKey("brandName"),
                            () -> assertThat(hit.highlights()).doesNotContainKey("name"),
                            () -> assertThat(hit.highlights()).doesNotContainKey("categoryName"),
                            // Nori가 '아디다스'를 '아디/다스'로 분해하므로 fragment는
                            // '<em>아디</em><em>다스</em>' 형태가 될 수 있다. <em> 태그를 제거한
                            // 원문이 '아디다스'를 포함하고, <em> 태그가 실제로 감싸져 있음을 검증.
                            () -> assertThat(hit.highlights().get("brandName"))
                                    .anyMatch(fragment -> fragment.contains("<em>")
                                            && fragment.contains("</em>")
                                            && fragment.replace("<em>", "").replace("</em>", "").contains("아디다스"))
                    );
                }
        );
    }

    @DisplayName("오타 관용 - 'Nile'(한 글자 오타)로 검색해도 'Nike' 관련 상품이 반환된다.")
    @Test
    void search_fuzziness_returnsResultsForSingleCharTypo() {
        // arrange & act — 한글 3자 '나이크'는 AUTO 정책상 fuzzy 비활성이므로
        // 영문 4자 'Nile'(Nike와 편집거리 1)로 AC-3 오타 관용을 검증한다.
        ProductSearchResult result = productSearchPort.searchProducts(
                "Nile", null, null, null, 0, 10, Sort.unsorted()
        );

        // assert
        assertAll(
                () -> assertThat(result.totalHits()).isGreaterThan(0),
                () -> assertThat(result.productIds()).contains(5L)
        );
    }

    @DisplayName("AUTO 정책 - 2자 이하 키워드는 오타 관용이 비활성화되어 정확 매칭만 동작한다.")
    @Test
    void search_fuzziness_shortKeyword_exactMatchOnly() {
        // arrange & act — '신발'은 categoryName 정확 매칭 대상
        ProductSearchResult result = productSearchPort.searchProducts(
                "신발", null, null, null, 0, 10, Sort.unsorted()
        );

        // assert
        assertAll(
                () -> assertThat(result.hits()).isNotEmpty(),
                () -> assertThat(result.hits())
                        .anySatisfy(hit -> assertThat(hit.highlights()).containsKey("categoryName"))
        );
    }

    @DisplayName("keyword blank - 전체 조회 시 모든 hit의 highlights는 빈 맵이다.")
    @Test
    void search_blankKeyword_allHitsHaveEmptyHighlights() {
        // arrange & act
        ProductSearchResult result = productSearchPort.searchProducts(
                null, null, null, null, 0, 10, Sort.unsorted()
        );

        // assert
        assertAll(
                () -> assertThat(result.hits()).hasSize(6),
                () -> assertThat(result.hits())
                        .allSatisfy(hit -> assertThat(hit.highlights()).isEmpty())
        );
    }
}
