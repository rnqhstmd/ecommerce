package com.loopers.domain.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
class ProductCacheTest {

    @TestConfiguration
    @EnableCaching
    static class TestCacheConfig {
        @Bean
        @Primary
        public CacheManager testCacheManager() {
            return new ConcurrentMapCacheManager("product");
        }
    }

    @Autowired
    private ProductService productService;

    @MockitoSpyBean
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private CacheManager cacheManager;

    private Product savedProduct;

    @BeforeEach
    void setUp() {
        Brand brand = brandRepository.save(Brand.create("Cache Brand"));
        savedProduct = productRepository.save(Product.create("Cache Product", 5000L, 50, brand.getId()));
    }

    @AfterEach
    void tearDown() {
        cacheManager.getCache("product").clear();
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("상품 단건 조회 시 캐시에 저장되어, 두 번째 조회에서는 DB를 조회하지 않는다.")
    @Test
    void getProduct_cacheHit() {
        Long productId = savedProduct.getId();

        // act - 첫 번째 조회 (DB 히트)
        Product first = productService.getProduct(productId);
        // act - 두 번째 조회 (캐시 히트)
        Product second = productService.getProduct(productId);

        // assert
        assertThat(first.getId()).isEqualTo(second.getId());
        verify(productRepository, times(1)).findById(productId);
    }

    @DisplayName("캐시에 저장된 상품을 직접 확인할 수 있다.")
    @Test
    void getProduct_cachedInCacheManager() {
        Long productId = savedProduct.getId();

        // act
        productService.getProduct(productId);

        // assert
        Object cached = cacheManager.getCache("product").get(productId).get();
        assertThat(cached).isNotNull();
        assertThat(cached).isInstanceOf(Product.class);
        assertThat(((Product) cached).getId()).isEqualTo(productId);
    }

    @DisplayName("캐시 무효화 후 다시 조회하면 DB에서 조회한다.")
    @Test
    void evictProductCache_thenDbHit() {
        Long productId = savedProduct.getId();

        // 첫 번째 조회 (캐시 저장)
        productService.getProduct(productId);
        assertThat(cacheManager.getCache("product").get(productId)).isNotNull();

        // 캐시 무효화
        productService.evictProductCache(productId);
        assertThat(cacheManager.getCache("product").get(productId)).isNull();

        // 다시 조회 (DB 히트)
        productService.getProduct(productId);

        verify(productRepository, times(2)).findById(productId);
    }
}
