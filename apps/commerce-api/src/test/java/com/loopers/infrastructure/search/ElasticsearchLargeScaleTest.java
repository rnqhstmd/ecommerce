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
@Tag("large-scale")
@EnabledIfSystemProperty(named = "test.large-scale", matches = "true")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ElasticsearchLargeScaleTest {

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

    private static final String[] BRAND_NAMES = {
            "나이키", "아디다스", "뉴발란스", "퓨마", "리복",
            "컨버스", "반스", "아식스", "미즈노", "언더아머",
            "프로스펙스", "휠라", "데상트", "살로몬", "호카",
            "온러닝", "브룩스", "스케쳐스", "르꼬끄", "라코스테"
    };

    private static final String[] CATEGORY_NAMES = {
            "운동화", "러닝화", "캐주얼화", "샌들", "부츠",
            "슬리퍼", "축구화", "농구화", "등산화", "워킹화"
    };

    private static final String[] MODIFIERS = {
            "에어", "울트라", "프로", "라이트", "맥스",
            "클래식", "스포츠", "프리미엄", "한정판", "베이직",
            "레트로", "테크", "플라이", "줌", "리액트",
            "부스트", "젤", "웨이브", "프레시폼", "퓨얼셀",
            "클라우드", "슈퍼노바", "트레일", "고어텍스", "방수",
            "경량", "쿠션", "안정", "뉴트럴", "모션",
            "엘리트", "레이서", "트레이너", "이지", "조깅",
            "마라톤", "하이킹", "크로스핏", "인도어", "아웃도어",
            "키즈", "여성용", "남성용", "유니섹스", "시즌한정",
            "콜라보", "리미티드", "에디션", "시그니처", "오리지널"
    };

    private static final int TOTAL_PRODUCTS = 500_000;
    private static final int BATCH_SIZE = 5_000;
    private static final int MEDIAN_RUNS = 5;
    private static final int LOG_INTERVAL = 50_000;

    private static boolean dataInitialized = false;
    private static final Map<String, Long> brandIdMap = new LinkedHashMap<>();
    private static final Map<String, Long> categoryIdMap = new LinkedHashMap<>();
    private static long mysqlInsertTimeMs = 0;
    private static long esIndexTimeMs = 0;

    @BeforeEach
    void setUp() {
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> cb.reset());
    }

    // ========================================================================
    // 1. 데이터 준비 + 건수 확인
    // ========================================================================

    @DisplayName("데이터 준비: 500,000건 삽입 및 건수 확인")
    @Test
    @Order(1)
    void setupData_500k() throws IOException {
        if (dataInitialized) {
            return;
        }

        System.out.println("\n[대규모 테스트] 500,000건 데이터 삽입 시작...");

        // -- 0) 기존 데이터 정리 (이전 실행 실패 시 잔존 데이터 제거) --
        productJpaRepository.deleteAll();
        categoryJpaRepository.deleteAll();
        brandJpaRepository.deleteAll();
        brandIdMap.clear();
        categoryIdMap.clear();

        // -- 1) 브랜드 20개 생성 --
        List<Brand> brands = new ArrayList<>();
        for (String brandName : BRAND_NAMES) {
            brands.add(Brand.create(brandName));
        }
        brands = brandJpaRepository.saveAll(brands);
        for (Brand brand : brands) {
            brandIdMap.put(brand.getName(), brand.getId());
        }

        // -- 2) 카테고리 10개 생성 --
        List<Category> categories = new ArrayList<>();
        for (String categoryName : CATEGORY_NAMES) {
            categories.add(Category.createRoot(categoryName));
        }
        categories = categoryJpaRepository.saveAll(categories);
        for (Category category : categories) {
            categoryIdMap.put(category.getName(), category.getId());
        }

        // -- 3) ES 인덱스 준비 --
        boolean indexExists = elasticsearchClient.indices()
                .exists(e -> e.index("products")).value();
        if (!indexExists) {
            try (var is = new org.springframework.core.io.ClassPathResource(
                    "elasticsearch/products-index-settings.json").getInputStream()) {
                elasticsearchClient.indices().create(c -> c.index("products").withJson(is));
            }
        }
        productSearchRepository.deleteAll();

        // -- 4) 상품 500,000건 생성 (MySQL + ES 배치) --
        Random random = ThreadLocalRandom.current();

        long mysqlStartTime = System.currentTimeMillis();
        long esAccumulatedTime = 0;

        List<Product> productBatch = new ArrayList<>(BATCH_SIZE);
        List<ProductDocument> documentBatch = new ArrayList<>(BATCH_SIZE);

        for (int i = 0; i < TOTAL_PRODUCTS; i++) {
            String brandName = BRAND_NAMES[i % BRAND_NAMES.length];
            String categoryName = CATEGORY_NAMES[i % CATEGORY_NAMES.length];
            String modifier = MODIFIERS[random.nextInt(MODIFIERS.length)];
            String productName = brandName + " " + modifier + " " + categoryName;

            Long brandId = brandIdMap.get(brandName);
            Long categoryId = categoryIdMap.get(categoryName);
            long price = 20_000L + random.nextInt(480_001); // 20,000 ~ 500,000
            int likeCount = random.nextInt(1_001); // 0 ~ 1000

            Product product = Product.create(productName, price, 100, brandId);
            product.updateCategoryId(categoryId);
            productBatch.add(product);

            if (productBatch.size() == BATCH_SIZE) {
                // MySQL 배치 저장
                List<Product> saved = productJpaRepository.saveAll(productBatch);

                // ES 문서 생성
                for (Product p : saved) {
                    String bName = BRAND_NAMES[(int) ((p.getId() - 1) % BRAND_NAMES.length)];
                    String cName = CATEGORY_NAMES[(int) ((p.getId() - 1) % CATEGORY_NAMES.length)];

                    ProductDocument doc = ProductDocument.builder()
                            .id(p.getId())
                            .name(p.getName())
                            .brandId(p.getBrandId())
                            .brandName(bName)
                            .categoryId(p.getCategoryId())
                            .categoryName(cName)
                            .price(p.getPriceValue())
                            .likeCount((long) likeCount)
                            .createdAt(p.getCreatedAt() != null
                                    ? p.getCreatedAt().toOffsetDateTime().toString() : null)
                            .deletedAt(null)
                            .build();
                    documentBatch.add(doc);
                }

                // ES 배치 저장
                long esBatchStart = System.currentTimeMillis();
                productSearchRepository.saveAll(documentBatch);
                esAccumulatedTime += (System.currentTimeMillis() - esBatchStart);

                productBatch.clear();
                documentBatch.clear();

                // 진행 상황 로깅
                int processed = i + 1;
                if (processed % LOG_INTERVAL == 0) {
                    System.out.printf("  삽입 진행: %,d / %,d%n", processed, TOTAL_PRODUCTS);
                }
            }
        }

        // 남은 배치 처리
        if (!productBatch.isEmpty()) {
            List<Product> saved = productJpaRepository.saveAll(productBatch);

            for (Product p : saved) {
                String bName = BRAND_NAMES[(int) ((p.getId() - 1) % BRAND_NAMES.length)];
                String cName = CATEGORY_NAMES[(int) ((p.getId() - 1) % CATEGORY_NAMES.length)];

                ProductDocument doc = ProductDocument.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .brandId(p.getBrandId())
                        .brandName(bName)
                        .categoryId(p.getCategoryId())
                        .categoryName(cName)
                        .price(p.getPriceValue())
                        .likeCount(0L)
                        .createdAt(p.getCreatedAt() != null
                                ? p.getCreatedAt().toOffsetDateTime().toString() : null)
                        .deletedAt(null)
                        .build();
                documentBatch.add(doc);
            }

            long esBatchStart = System.currentTimeMillis();
            productSearchRepository.saveAll(documentBatch);
            esAccumulatedTime += (System.currentTimeMillis() - esBatchStart);

            productBatch.clear();
            documentBatch.clear();
        }

        long mysqlTotalTime = System.currentTimeMillis() - mysqlStartTime;
        mysqlInsertTimeMs = mysqlTotalTime - esAccumulatedTime; // MySQL 순수 시간
        esIndexTimeMs = esAccumulatedTime;

        // ES 인덱스 refresh (마지막에 1회만)
        elasticsearchClient.indices().refresh(r -> r.index("products"));

        // 건수 확인
        long esCount = elasticsearchClient.count(c -> c.index("products")).count();
        long mysqlCount = productJpaRepository.count();

        assertThat(mysqlCount).isEqualTo(TOTAL_PRODUCTS);
        assertThat(esCount).isEqualTo(TOTAL_PRODUCTS);

        dataInitialized = true;

        // 결과 출력
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("╔══════════════════════════════════════════════════════════════════════╗\n");
        sb.append("║        ES vs MySQL LIKE 대규모 성능/품질 비교 (500,000건)            ║\n");
        sb.append("╠══════════════════════════════════════════════════════════════════════╣\n");
        sb.append("║                                                                    ║\n");
        sb.append("║ [데이터 준비]                                                       ║\n");
        sb.append(String.format("║ MySQL: %,d건 (삽입 소요: %.1fs)%n", mysqlCount, mysqlInsertTimeMs / 1000.0));
        sb.append(String.format("║ ES:    %,d건 (인덱싱 소요: %.1fs)%n", esCount, esIndexTimeMs / 1000.0));
        sb.append("║                                                                    ║\n");
        sb.append("╚══════════════════════════════════════════════════════════════════════╝\n");
        System.out.println(sb);
    }

    // ========================================================================
    // 2. 성능 비교 (5개 키워드, 5회 반복 median)
    // ========================================================================

    @DisplayName("성능 비교: ES vs MySQL LIKE — 500,000건, 5회 median")
    @Test
    @Order(2)
    void performanceComparison_500k() throws IOException {
        ensureDataInitialized();

        String[] keywords = {"나이키 운동화", "프리미엄 러닝화", "에어 맥스", "울트라 부스트", "고어텍스 등산화"};

        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("╔══════════════════════════════════════════════════════════════════════╗\n");
        sb.append("║ [성능 비교 — 키워드 검색 (5회 median)]                               ║\n");
        sb.append("╠══════════════════════════════════════════════════════════════════════╣\n");
        sb.append(String.format("║ %-22s│ %-10s│ %-10s│ %-10s║%n",
                " 키워드", " ES", " MySQL", " 개선율"));
        sb.append("║ ─────────────────────┼──────────┼──────────┼──────────║\n");

        for (String keyword : keywords) {
            // ES 검색 시간 측정 (5회)
            long[] esTimes = new long[MEDIAN_RUNS];
            long[] esHits = new long[MEDIAN_RUNS];
            for (int run = 0; run < MEDIAN_RUNS; run++) {
                long start = System.nanoTime();
                ProductSearchResult esResult = productSearchPort.searchProducts(
                        keyword, null, null, null, 0, 20, Sort.unsorted());
                esTimes[run] = (System.nanoTime() - start) / 1_000_000;
                esHits[run] = esResult.totalHits();
            }

            // MySQL LIKE 검색 시간 측정 (5회)
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

            sb.append(String.format("║ %-20s│ %6dms  │ %6dms  │ %6.1f배  ║%n",
                    " " + keyword, esMedian, mysqlMedian, improvement));

            assertThat(esHits[0]).as("ES 검색 결과가 존재해야 합니다: " + keyword).isGreaterThan(0);
        }

        sb.append("║                                                                    ║\n");
        sb.append("╚══════════════════════════════════════════════════════════════════════╝\n");
        System.out.println(sb);
    }

    // ========================================================================
    // 3. 형태소 분석 품질 비교
    // ========================================================================

    @DisplayName("검색 품질 비교: 형태소 분석 — 500,000건")
    @Test
    @Order(3)
    void morphemeQuality_500k() throws IOException {
        ensureDataInitialized();

        String[] keywords = {"운동화를", "프리미엄의", "러닝화에서", "등산화는"};
        String[] baseWords = {"운동화", "프리미엄", "러닝화", "등산화"};

        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("╔══════════════════════════════════════════════════════════════════════╗\n");
        sb.append("║ [검색 품질 — 형태소 분석]                                            ║\n");
        sb.append("╠══════════════════════════════════════════════════════════════════════╣\n");
        sb.append(String.format("║ %-18s│ %-12s│ %-12s│ %-18s║%n",
                " 검색어", " ES 결과", " MySQL 결과", " 차이"));
        sb.append("║ ─────────────────┼────────────┼────────────┼──────────────────║\n");

        for (int i = 0; i < keywords.length; i++) {
            String keyword = keywords[i];
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
                diff = "ES가 " + (esCount - mysqlCount) + "건 더";
            } else {
                diff = "동일";
            }

            sb.append(String.format("║ %-16s│ %,10d건│ %,10d건│ %-16s║%n",
                    " " + keyword, esCount, mysqlCount, " " + diff));

            // ES는 조사를 분리하여 원형을 매칭하므로 결과가 있어야 함
            assertThat(esCount)
                    .as("ES는 '" + keyword + "'에서 조사를 분리하여 '" + base + "' 매칭 결과가 존재해야 합니다")
                    .isGreaterThan(0);
            // MySQL LIKE는 조사가 붙은 정확한 문자열이 상품명에 없으므로 0건
            assertThat(mysqlCount)
                    .as("MySQL LIKE는 '" + keyword + "' 정확히 포함하는 상품이 없어 0건이어야 합니다")
                    .isEqualTo(0);
        }

        sb.append("║                                                                    ║\n");
        sb.append("╚══════════════════════════════════════════════════════════════════════╝\n");
        System.out.println(sb);
    }

    // ========================================================================
    // 4. 페이지 깊이별 성능 비교
    // ========================================================================

    @DisplayName("페이지 깊이별 성능 비교: ES offset vs MySQL offset — 500,000건")
    @Test
    @Order(4)
    void deepPagingComparison_500k() throws IOException {
        ensureDataInitialized();

        int[] pages = {0, 10, 100, 499}; // page*size < max_result_window(10,000)
        String searchKeyword = "나이키";
        int pageSize = 20;

        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("╔══════════════════════════════════════════════════════════════════════╗\n");
        sb.append("║ [깊은 페이지 성능 비교] 키워드: \"" + searchKeyword + "\"");
        sb.append(String.format("%" + (37 - searchKeyword.length() * 2) + "s║%n", ""));
        sb.append("╠══════════════════════════════════════════════════════════════════════╣\n");
        sb.append(String.format("║ %-18s│ %-10s│ %-10s│ %-10s║%n",
                " 페이지", " ES", " MySQL", " 개선율"));
        sb.append("║ ─────────────────┼──────────┼──────────┼──────────║\n");

        for (int page : pages) {
            // ES 검색 시간 측정 (5회 median)
            long[] esTimes = new long[MEDIAN_RUNS];
            for (int run = 0; run < MEDIAN_RUNS; run++) {
                long start = System.nanoTime();
                productSearchPort.searchProducts(
                        searchKeyword, null, null, null, page, pageSize, Sort.unsorted());
                esTimes[run] = (System.nanoTime() - start) / 1_000_000;
            }

            // MySQL LIKE 검색 시간 측정 (5회 median)
            long[] mysqlTimes = new long[MEDIAN_RUNS];
            for (int run = 0; run < MEDIAN_RUNS; run++) {
                long start = System.nanoTime();
                productRepository.findProducts(
                        new ProductSearchCondition(null, searchKeyword, null, null,
                                PageRequest.of(page, pageSize)));
                mysqlTimes[run] = (System.nanoTime() - start) / 1_000_000;
            }

            long esMedian = median(esTimes);
            long mysqlMedian = median(mysqlTimes);
            double improvement = (double) mysqlMedian / Math.max(esMedian, 1);

            sb.append(String.format("║ %-16s│ %6dms  │ %6dms  │ %6.1f배  ║%n",
                    " page=" + page, esMedian, mysqlMedian, improvement));
        }

        sb.append("║                                                                    ║\n");
        sb.append("╚══════════════════════════════════════════════════════════════════════╝\n");
        System.out.println(sb);
    }

    // ========================================================================
    // 유틸리티
    // ========================================================================

    private void ensureDataInitialized() throws IOException {
        if (!dataInitialized) {
            setupData_500k();
        }
    }

    private long median(long[] values) {
        long[] sorted = values.clone();
        Arrays.sort(sorted);
        return sorted[sorted.length / 2];
    }

    @AfterAll
    static void resetFlag() {
        dataInitialized = false;
        brandIdMap.clear();
        categoryIdMap.clear();
    }
}
