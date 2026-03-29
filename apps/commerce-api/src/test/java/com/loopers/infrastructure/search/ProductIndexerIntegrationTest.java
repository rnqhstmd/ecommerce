package com.loopers.infrastructure.search;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.product.*;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
class ProductIndexerIntegrationTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private ProductSearchRepository productSearchRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @AfterEach
    void tearDown() {
        productSearchRepository.deleteAll();
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("ProductCreatedEvent 발행 시 ES에 상품 문서가 생성된다.")
    @Test
    void handleProductCreated_indexesDocumentInEs() {
        // arrange & act — 트랜잭션 커밋 후 이벤트 리스너가 동작하도록 TransactionTemplate 사용
        Long productId = transactionTemplate.execute(status -> {
            Brand brand = brandRepository.save(Brand.create("나이키"));
            Product product = productRepository.save(Product.create("나이키 운동화", 89000L, 50, brand.getId()));
            eventPublisher.publishEvent(new ProductCreatedEvent(product.getId()));
            return product.getId();
        });

        // ES 인덱스 반영 대기
        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}

        // assert
        Optional<ProductDocument> document = productSearchRepository.findById(productId);
        assertAll(
                () -> assertThat(document).isPresent(),
                () -> assertThat(document.get().getName()).isEqualTo("나이키 운동화"),
                () -> assertThat(document.get().getPrice()).isEqualTo(89000L),
                () -> assertThat(document.get().getBrandName()).isEqualTo("나이키")
        );
    }

    @DisplayName("ProductUpdatedEvent 발행 시 ES 문서가 업데이트된다.")
    @Test
    void handleProductUpdated_updatesDocumentInEs() {
        // arrange — 먼저 상품 생성
        Long productId = transactionTemplate.execute(status -> {
            Brand brand = brandRepository.save(Brand.create("아디다스"));
            Product product = productRepository.save(Product.create("아디다스 런닝화", 120000L, 30, brand.getId()));
            eventPublisher.publishEvent(new ProductCreatedEvent(product.getId()));
            return product.getId();
        });

        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}

        // act — 상품 수정 후 이벤트 발행
        transactionTemplate.executeWithoutResult(status -> {
            Product product = productRepository.findById(productId).orElseThrow();
            product.updateName("아디다스 울트라부스트");
            productRepository.save(product);
            eventPublisher.publishEvent(new ProductUpdatedEvent(productId));
        });

        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}

        // assert
        Optional<ProductDocument> document = productSearchRepository.findById(productId);
        assertAll(
                () -> assertThat(document).isPresent(),
                () -> assertThat(document.get().getName()).isEqualTo("아디다스 울트라부스트")
        );
    }

    @DisplayName("ProductDeletedEvent 발행 시 ES 문서가 삭제된다.")
    @Test
    void handleProductDeleted_deletesDocumentFromEs() {
        // arrange — ES에 직접 문서 삽입
        ProductDocument document = ProductDocument.builder()
                .id(999L).name("삭제 대상 상품").brandId(10L).brandName("테스트브랜드")
                .price(50000L).likeCount(0L).build();
        productSearchRepository.save(document);

        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}

        assertThat(productSearchRepository.findById(999L)).isPresent();

        // act — TransactionalEventListener는 AFTER_COMMIT이므로 트랜잭션 내에서 발행
        transactionTemplate.executeWithoutResult(status -> {
            eventPublisher.publishEvent(new ProductDeletedEvent(999L));
        });

        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}

        // assert
        assertThat(productSearchRepository.findById(999L)).isEmpty();
    }
}
