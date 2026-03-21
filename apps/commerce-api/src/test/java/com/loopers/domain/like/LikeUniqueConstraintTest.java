package com.loopers.domain.like;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserService;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class LikeUniqueConstraintTest {

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private String userId;
    private Long productId;

    @BeforeEach
    void setUp() {
        userService.signUp("uniqueuser", "unique@mail.com", "1990-01-01", Gender.MALE);
        userId = "uniqueuser";

        Brand brand = brandRepository.save(Brand.create("Unique Brand"));
        Product product = productRepository.save(Product.create("Unique Product", 1000L, 10, brand.getId()));
        productId = product.getId();
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("동일 사용자가 같은 상품에 중복 좋아요를 등록하면 DataIntegrityViolationException이 발생한다.")
    @Test
    void duplicateLike_throwsDataIntegrityViolation() {
        // arrange - 첫 번째 좋아요 저장
        Like firstLike = Like.create(userId, productId);
        likeRepository.save(firstLike);

        // act & assert - 두 번째 좋아요 저장 시 예외 발생
        Like secondLike = Like.create(userId, productId);
        assertThatThrownBy(() -> likeRepository.save(secondLike))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
