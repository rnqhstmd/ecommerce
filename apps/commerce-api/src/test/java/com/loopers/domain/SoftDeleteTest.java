package com.loopers.domain;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SoftDeleteTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("소프트 삭제된 엔티티는 조회되지 않는다.")
    @Test
    void softDeletedEntity_isNotFoundByQuery() {
        // arrange
        Brand brand = brandRepository.save(Brand.create("Soft Delete Brand"));
        Product product = productRepository.save(Product.create("Soft Delete Product", 1000L, 10, brand.getId()));
        Long productId = product.getId();

        // act - soft delete
        product.delete();
        productRepository.save(product);

        // assert - 조회 시 empty
        Optional<Product> found = productRepository.findById(productId);
        assertThat(found).isEmpty();
    }
}
