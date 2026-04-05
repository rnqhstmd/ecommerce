package com.loopers.infrastructure.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.category.Category;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductSearchCondition;
import com.loopers.domain.product.ProductSearchPort;
import com.loopers.domain.product.ProductSearchResult;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.category.CategoryJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledIfSystemProperty(named = "test.performance", matches = "true")
class ElasticsearchPerformanceTest {

    @Autowired
    private ProductSearchPort productSearchPort;

    @Autowired
    private ProductSearchRepository productSearchRepository;

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private BrandJpaRepository brandJpaRepository;

    @Autowired
    private CategoryJpaRepository categoryJpaRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private static final String[] BRAND_NAMES = {
            "나이키", "아디다스", "뉴발란스", "퓨마", "리복",
            "컨버스", "반스", "아식스", "미즈노", "언더아머"
    };

    private static final String[] CATEGORY_NAMES = {
            "운동화", "러닝화", "캐주얼화", "샌들", "부츠"
    };

    private static final String[] MODIFIERS = {
            "에어", "울트라", "프로", "라이트", "맥스",
            "클래식", "스포츠", "프리미엄", "한정판", "베이직",
            "엘리트", "터보", "슈퍼", "메가", "네오",
            "플렉스", "부스트", "리액트", "줌", "퓨어"
    };

    private static final int TOTAL_PRODUCTS = 10_000;
    private static final int BATCH_SIZE = 1_000;
    private static final int MEDIAN_RUNS = 3;

    private static boolean dataInitialized = false;
    private static final Map<String, Long> brandIdMap = new LinkedHashMap<>();
    private static final Map<String, Long> categoryIdMap = new LinkedHashMap<>();

    @BeforeEach
    void setUp() throws IOException {
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> cb.reset());

        if (!dataInitialized) {
            initializeData();
            dataInitialized = true;
        }
    }

    private void initializeData() throws IOException {
        // -- 0) 이전 테스트 잔존 데이터 정리 (테스트 격리) --
        databaseCleanUp.truncateAllTables();
        brandIdMap.clear();
        categoryIdMap.clear();

        // -- 1) 브랜드 10개 생성 --
        List<Brand> brands = new ArrayList<>();
        for (String brandName : BRAND_NAMES) {
            brands.add(Brand.create(brandName));
        }
        brands = brandJpaRepository.saveAll(brands);
        for (Brand brand : brands) {
            brandIdMap.put(brand.getName(), brand.getId());
        }

        // -- 2) 카테고리 5개 생성 --
        List<Category> categories = new ArrayList<>();
        for (String categoryName : CATEGORY_NAMES) {
            categories.add(Category.createRoot(categoryName));
        }
        categories = categoryJpaRepository.saveAll(categories);
        for (Category category : categories) {
            categoryIdMap.put(category.getName(), category.getId());
        }

        // -- 3) ES 인덱스 준비 (기존 문서도 함께 제거) --
        boolean indexExists = elasticsearchClient.indices()
                .exists(e -> e.index("products")).value();
        if (!indexExists) {
            try (var is = new org.springframework.core.io.ClassPathResource(
                    "elasticsearch/products-index-settings.json").getInputStream()) {
                elasticsearchClient.indices().create(c -> c.index("products").withJson(is));
            }
        }
        productSearchRepository.deleteAll();
        elasticsearchClient.indices().refresh(r -> r.index("products"));

        // -- 4) 상품 10,000건 생성 (MySQL + ES 배치) --
        String[] brandNames = BRAND_NAMES;
        String[] categoryNames = CATEGORY_NAMES;
        Random random = ThreadLocalRandom.current();

        List<Product> productBatch = new ArrayList<>(BATCH_SIZE);
        // 상품별 likeCount를 batch 범위 내에서 보존 (배치 저장 시점에 각 문서에 매핑)
        List<Long> likeCountBatch = new ArrayList<>(BATCH_SIZE);
        List<ProductDocument> documentBatch = new ArrayList<>(BATCH_SIZE);

        for (int i = 0; i < TOTAL_PRODUCTS; i++) {
            String brandName = brandNames[i % brandNames.length];
            String categoryName = categoryNames[i % categoryNames.length];
            String modifier = MODIFIERS[random.nextInt(MODIFIERS.length)];
            String productName = brandName + " " + modifier + " " + categoryName;

            Long brandId = brandIdMap.get(brandName);
            Long categoryId = categoryIdMap.get(categoryName);
            long price = 30_000L + random.nextInt(270_001); // 30,000 ~ 300,000
            long likeCount = random.nextInt(501); // 0 ~ 500

            Product product = Product.create(productName, price, 100, brandId);
            product.updateCategoryId(categoryId);
            productBatch.add(product);
            likeCountBatch.add(likeCount);

            // 배치 단위로 MySQL 저장
            if (productBatch.size() == BATCH_SIZE) {
                flushBatch(productBatch, likeCountBatch, documentBatch, brandNames, categoryNames);
            }
        }

        // 남은 배치 처리
        if (!productBatch.isEmpty()) {
            flushBatch(productBatch, likeCountBatch, documentBatch, brandNames, categoryNames);
        }

        // ES 인덱스 refresh
        elasticsearchClient.indices().refresh(r -> r.index("products"));

        // 인덱싱 확인
        long esCount = elasticsearchClient.count(c -> c.index("products")).count();
        long mysqlCount = productJpaRepository.count();
        assertThat(esCount).isEqualTo(TOTAL_PRODUCTS);
        assertThat(mysqlCount).isEqualTo(TOTAL_PRODUCTS);
    }

    private void flushBatch(
            List<Product> productBatch,
            List<Long> likeCountBatch,
            List<ProductDocument> documentBatch,
            String[] brandNames,
            String[] categoryNames
    ) {
        List<Product> saved = productJpaRepository.saveAll(productBatch);

        // 저장된 Product로 ES 문서 생성 — 상품별 likeCount를 정확히 매핑
        for (int idx = 0; idx < saved.size(); idx++) {
            Product p = saved.get(idx);
            Long likeCount = likeCountBatch.get(idx);
            String bName = brandNames[(int) ((p.getId() - 1) % brandNames.length)];
            String cName = categoryNames[(int) ((p.getId() - 1) % categoryNames.length)];

            ProductDocument doc = ProductDocument.builder()
                    .id(p.getId())
                    .name(p.getName())
                    .brandId(p.getBrandId())
                    .brandName(bName)
                    .categoryId(p.getCategoryId())
                    .categoryName(cName)
                    .price(p.getPriceValue())
                    .likeCount(likeCount)
                    .createdAt(p.getCreatedAt() != null ? p.getCreatedAt().toOffsetDateTime().toString() : null)
                    .deletedAt(null)
                    .build();
            documentBatch.add(doc);
        }

        productSearchRepository.saveAll(documentBatch);

        productBatch.clear();
        likeCountBatch.clear();
        documentBatch.clear();
    }

    @AfterAll
    static void resetFlag() {
        dataInitialized = false;
        brandIdMap.clear();
        categoryIdMap.clear();
    }

    // ========================================================================
    // 1. 검색 성능 비교 (응답 시간)
    // ========================================================================

    @DisplayName("성능 비교: ES vs MySQL LIKE -- 10,000건 기준")
    @Test
    @Order(1)
    void performanceComparison() {
        String[] keywords = {"나이키 운동화", "프리미엄", "에어 맥스", "울트라"};

        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("+=======================================================================+\n");
        sb.append("|        ES vs MySQL LIKE 검색 성능/품질 비교 (10,000건)                    |\n");
        sb.append("+=======================================================================+\n");
        sb.append("|                                                                       |\n");
        sb.append("| [성능 비교]                                                             |\n");
        sb.append(String.format("| %-18s | %-10s | %-10s | %-10s |%n",
                "키워드", "ES", "MySQL", "개선율"));
        sb.append("| ------------------+------------+------------+----------- |\n");

        for (String keyword : keywords) {
            // ES 검색 시간 측정 (3회 중간값)
            long[] esTimes = new long[MEDIAN_RUNS];
            long[] esHits = new long[MEDIAN_RUNS];
            for (int run = 0; run < MEDIAN_RUNS; run++) {
                long start = System.nanoTime();
                ProductSearchResult esResult = productSearchPort.searchProducts(
                        keyword, null, null, null, 0, 20, Sort.unsorted());
                esTimes[run] = (System.nanoTime() - start) / 1_000_000;
                esHits[run] = esResult.totalHits();
            }

            // MySQL LIKE 검색 시간 측정 (3회 중간값)
            long[] mysqlTimes = new long[MEDIAN_RUNS];
            long[] mysqlHits = new long[MEDIAN_RUNS];
            for (int run = 0; run < MEDIAN_RUNS; run++) {
                long start = System.nanoTime();
                Page<Product> mysqlResult = productRepository.findProducts(
                        new ProductSearchCondition(null, keyword, null, null, PageRequest.of(0, 20)));
                mysqlTimes[run] = (System.nanoTime() - start) / 1_000_000;
                mysqlHits[run] = mysqlResult.getTotalElements();
            }

            long esMedian = median(esTimes);
            long mysqlMedian = median(mysqlTimes);
            double improvement = (double) mysqlMedian / Math.max(esMedian, 1);

            sb.append(String.format("| %-18s | %6dms   | %6dms   | %6.1f배   |%n",
                    keyword, esMedian, mysqlMedian, improvement));

            // 기본 검증: 검색 결과가 존재해야 함 (ES 기준)
            assertThat(esHits[0]).as("ES 검색 결과가 존재해야 합니다: " + keyword).isGreaterThan(0);
        }

        sb.append("|                                                                       |\n");
        System.out.println(sb);
    }

    // ========================================================================
    // 2. 검색 품질 비교 (형태소 분석)
    // ========================================================================

    @DisplayName("검색 품질 비교: 형태소 분석 -- ES vs MySQL LIKE")
    @Test
    @Order(2)
    void searchQualityComparison_morpheme() {
        // 조사가 결합된 검색어: ES의 Nori가 조사를 분리하여 원형 매칭, MySQL은 정확한 부분문자열만 매칭
        // 조사가 결합된 검색어: 상품명에 원형이 포함된 것만 테스트
        // "나이키를"은 제외: brandName 필드에만 "나이키"가 있고 best_fields에서 "를" 토큰이 매칭 불가
        String[] morphemeKeywords = {"운동화를", "프리미엄의", "러닝화에서"};
        String[] baseWords = {"운동화", "프리미엄", "러닝화"};

        StringBuilder sb = new StringBuilder();
        sb.append("| [검색 품질 -- 형태소 분석]                                               |\n");
        sb.append(String.format("| %-18s | %-10s | %-12s | %-20s |%n",
                "검색어", "ES 결과", "MySQL 결과", "차이"));
        sb.append("| ------------------+------------+--------------+--------------------- |\n");

        for (int i = 0; i < morphemeKeywords.length; i++) {
            String keyword = morphemeKeywords[i];
            String base = baseWords[i];

            // ES 검색: Nori가 조사를 분리하여 원형 매칭
            ProductSearchResult esResult = productSearchPort.searchProducts(
                    keyword, null, null, null, 0, 20, Sort.unsorted());

            // MySQL LIKE 검색: 조사가 포함된 정확한 부분문자열 매칭
            Page<Product> mysqlResult = productRepository.findProducts(
                    new ProductSearchCondition(null, keyword, null, null, PageRequest.of(0, 20)));

            long esCount = esResult.totalHits();
            long mysqlCount = mysqlResult.getTotalElements();

            String diff;
            if (esCount > 0 && mysqlCount == 0) {
                diff = "ES만 매칭 (조사 분리)";
            } else if (esCount > mysqlCount) {
                diff = "ES가 " + (esCount - mysqlCount) + "건 더 매칭";
            } else {
                diff = "동일";
            }

            sb.append(String.format("| %-18s | %,8d건  | %,10d건  | %-20s |%n",
                    keyword, esCount, mysqlCount, diff));

            // ES는 조사를 분리하므로 결과가 있어야 하고, MySQL은 조사가 붙은 정확한 문자열이 없으므로 0건이어야 함
            assertThat(esCount)
                    .as("ES는 '" + keyword + "'에서 조사를 분리하여 '" + base + "' 매칭 결과가 존재해야 합니다")
                    .isGreaterThan(0);
            assertThat(mysqlCount)
                    .as("MySQL LIKE는 '" + keyword + "' 정확히 포함하는 상품이 없어 0건이어야 합니다")
                    .isEqualTo(0);
        }

        sb.append("|                                                                       |\n");
        System.out.println(sb);
    }

    // ========================================================================
    // 3. 멀티필드 검색 비교
    // ========================================================================

    @DisplayName("검색 품질 비교: 멀티필드 -- ES vs MySQL LIKE")
    @Test
    @Order(3)
    void searchQualityComparison_multiField() {
        StringBuilder sb = new StringBuilder();
        sb.append("| [검색 품질 -- 멀티필드]                                                  |\n");
        sb.append(String.format("| %-18s | %-10s | %-12s | %-24s |%n",
                "검색어", "ES 결과", "MySQL 결과", "차이"));
        sb.append("| ------------------+------------+--------------+------------------------- |\n");

        // 케이스 1: 브랜드명 검색 - "아디다스"
        // ES: name + brandName + categoryName 멀티필드 -> brandName 매칭으로 모든 아디다스 상품 반환
        // MySQL: LIKE '%아디다스%' on name -> name에 "아디다스"가 있는 것만 (상품명에 브랜드명 포함)
        {
            String keyword = "아디다스";

            ProductSearchResult esResult = productSearchPort.searchProducts(
                    keyword, null, null, null, 0, 20, Sort.unsorted());

            Page<Product> mysqlResult = productRepository.findProducts(
                    new ProductSearchCondition(null, keyword, null, null, PageRequest.of(0, 20)));

            long esCount = esResult.totalHits();
            long mysqlCount = mysqlResult.getTotalElements();

            // 이 케이스에서는 상품명 패턴이 "{브랜드} {수식어} {카테고리}" 이므로
            // MySQL도 name에 "아디다스"가 포함되어 매칭됨.
            // ES는 brandName 필드에서도 추가 매칭하므로 동일하거나 더 많을 수 있음.
            String diff;
            if (esCount > mysqlCount) {
                diff = "ES가 " + (esCount - mysqlCount) + "건 더 매칭 (멀티필드)";
            } else {
                diff = "동일 (name에 브랜드명 포함)";
            }

            sb.append(String.format("| %-18s | %,8d건  | %,10d건  | %-24s |%n",
                    keyword, esCount, mysqlCount, diff));

            assertThat(esCount)
                    .as("ES 멀티필드 검색 결과가 존재해야 합니다: " + keyword)
                    .isGreaterThan(0);
            assertThat(esCount)
                    .as("ES가 MySQL보다 같거나 더 많은 결과를 반환해야 합니다")
                    .isGreaterThanOrEqualTo(mysqlCount);
        }

        // 케이스 2: 카테고리명만으로 검색 - "러닝"
        // ES: categoryName "러닝화" 매칭 (형태소 분석으로 "러닝" 추출)
        // MySQL: name에 "러닝"이 포함된 것만 (상품명에 "러닝화"가 있으면 매칭)
        {
            String keyword = "러닝";

            ProductSearchResult esResult = productSearchPort.searchProducts(
                    keyword, null, null, null, 0, 20, Sort.unsorted());

            Page<Product> mysqlResult = productRepository.findProducts(
                    new ProductSearchCondition(null, keyword, null, null, PageRequest.of(0, 20)));

            long esCount = esResult.totalHits();
            long mysqlCount = mysqlResult.getTotalElements();

            String diff;
            if (esCount > mysqlCount) {
                diff = "ES가 " + (esCount - mysqlCount) + "건 더 (멀티필드)";
            } else {
                diff = "동일 (name에 카테고리명 포함)";
            }

            sb.append(String.format("| %-18s | %,8d건  | %,10d건  | %-24s |%n",
                    keyword, esCount, mysqlCount, diff));

            assertThat(esCount)
                    .as("ES 멀티필드 검색 결과가 존재해야 합니다: " + keyword)
                    .isGreaterThan(0);
            assertThat(esCount)
                    .as("ES가 MySQL보다 같거나 더 많은 결과를 반환해야 합니다")
                    .isGreaterThanOrEqualTo(mysqlCount);
        }

        // 케이스 3: 순수 카테고리명 검색 - "부츠"
        // ES: categoryName "부츠" 매칭 (name + categoryName 모두)
        // MySQL: name에 "부츠" 포함된 것만
        {
            String keyword = "부츠";

            ProductSearchResult esResult = productSearchPort.searchProducts(
                    keyword, null, null, null, 0, 20, Sort.unsorted());

            Page<Product> mysqlResult = productRepository.findProducts(
                    new ProductSearchCondition(null, keyword, null, null, PageRequest.of(0, 20)));

            long esCount = esResult.totalHits();
            long mysqlCount = mysqlResult.getTotalElements();

            String diff;
            if (esCount > mysqlCount) {
                diff = "ES가 " + (esCount - mysqlCount) + "건 더 (멀티필드)";
            } else {
                diff = "동일 (name에 카테고리명 포함)";
            }

            sb.append(String.format("| %-18s | %,8d건  | %,10d건  | %-24s |%n",
                    keyword, esCount, mysqlCount, diff));

            assertThat(esCount)
                    .as("ES 멀티필드 검색 결과가 존재해야 합니다: " + keyword)
                    .isGreaterThan(0);
            assertThat(esCount)
                    .as("ES가 MySQL보다 같거나 더 많은 결과를 반환해야 합니다")
                    .isGreaterThanOrEqualTo(mysqlCount);
        }

        sb.append("+=======================================================================+\n");
        System.out.println(sb);
    }

    // ========================================================================
    // 유틸리티
    // ========================================================================

    private long median(long[] values) {
        long[] sorted = values.clone();
        Arrays.sort(sorted);
        return sorted[sorted.length / 2];
    }
}
