package com.loopers.domain.like;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
class LikeServiceIntegrationTest {
    @Autowired
    private LikeService likeService;

    @MockitoSpyBean
    private LikeRepository likeRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private String userId1, userId2;
    private Long productId1, productId2;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성
        User user1 = userService.signUp("likeUser01", "like@mail.com", "1990-01-01", Gender.MALE);
        User user2 = userService.signUp("likeUser02", "like@mail.com", "1990-01-02", Gender.FEMALE);
        userId1 = user1.getUserIdValue();
        userId2 = user2.getUserIdValue();

        // 테스트용 상품 생성
        Brand brand = brandRepository.save(Brand.create("Like Brand"));
        Product product1 = productRepository.save(Product.create("Like Product 1", 1000L, 10, brand.getId()));
        Product product2 = productRepository.save(Product.create("Like Product 2", 2000L, 20, brand.getId()));
        productId1 = product1.getId();
        productId2 = product2.getId();
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("좋아요 등록/취소/중복 방지")
    @Nested
    class CoreLikeFlow {

        @DisplayName("좋아요를 등록할 수 있다.")
        @Test
        void addLike() {
            // act
            likeService.addLike(userId1, productId1);

            // assert
            verify(likeRepository, times(1)).save(any(Like.class));
            assertThat(likeRepository.existsByUserIdAndProductId(userId1, productId1)).isTrue();
        }

        @DisplayName("중복 좋아요 방지를 위한 멱등성 처리가 구현되었다.")
        @Test
        void addLike_idempotent() {
            // arrange
            likeService.addLike(userId1, productId1); // 1번째 호출

            // act
            likeService.addLike(userId1, productId1); // 2번째 (중복) 호출

            // assert
            verify(likeRepository, times(1)).save(any(Like.class));
            verify(likeRepository, times(2)).existsByUserIdAndProductId(userId1, productId1);
        }

        @DisplayName("좋아요를 취소할 수 있다.")
        @Test
        void removeLike() {
            // arrange (미리 좋아요 추가)
            likeService.addLike(userId1, productId1);
            assertThat(likeRepository.existsByUserIdAndProductId(userId1, productId1)).isTrue();

            // act
            likeService.removeLike(userId1, productId1);

            // assert
            verify(likeRepository, times(1)).delete(any(Like.class));
            assertThat(likeRepository.existsByUserIdAndProductId(userId1, productId1)).isFalse();
        }

        @DisplayName("좋아요를 누르지 않은 상품을 취소해도 에러가 발생하지 않는다.")
        @Test
        void removeLike_nonExistent() {
            // arrange
            assertThat(likeRepository.existsByUserIdAndProductId(userId1, productId1)).isFalse();

            // act
            likeService.removeLike(userId1, productId1);

            // assert
            verify(likeRepository, never()).delete(any(Like.class));
        }
    }


    @DisplayName("좋아요 수 조회")
    @Nested
    class GetLikeCount {

        @DisplayName("특정 상품의 좋아요 수를 조회할 수 있다.")
        @Test
        void getLikeCount() {
            // arrange
            likeService.addLike(userId1, productId1); // product1 (1)
            likeService.addLike(userId2, productId1); // product1 (2)
            likeService.addLike(userId1, productId2); // product2 (1)

            // act
            Long count1 = likeService.getLikeCount(productId1);
            Long count2 = likeService.getLikeCount(productId2);

            // assert
            verify(likeRepository, times(1)).countByProductId(productId1);
            verify(likeRepository, times(1)).countByProductId(productId2);
            assertThat(count1).isEqualTo(2L);
            assertThat(count2).isEqualTo(1L);
        }
    }

    @DisplayName("사용자별 좋아요 목록 조회")
    @Nested
    class GetLikesByUserId {

        @DisplayName("사용자의 좋아요 목록을 조회할 수 있다.")
        @Test
        void getLikesByUserId() {
            // arrange
            likeService.addLike(userId1, productId1);
            likeService.addLike(userId1, productId2);

            // act
            java.util.List<Like> likes = likeService.getLikesByUserId(userId1);

            // assert
            assertThat(likes).hasSize(2);
            assertThat(likes).extracting(Like::getProductId)
                    .containsExactlyInAnyOrder(productId1, productId2);
        }
    }

    @DisplayName("좋아요 여부 단건 조회")
    @Nested
    class GetIsLiked {

        @DisplayName("좋아요한 상품은 true를 반환한다.")
        @Test
        void getIsLiked_returnsTrue() {
            // arrange
            likeService.addLike(userId1, productId1);

            // act
            Boolean result = likeService.getIsLiked(userId1, productId1);

            // assert
            assertThat(result).isTrue();
        }

        @DisplayName("좋아요하지 않은 상품은 false를 반환한다.")
        @Test
        void getIsLiked_returnsFalse() {
            // act
            Boolean result = likeService.getIsLiked(userId1, productId1);

            // assert
            assertThat(result).isFalse();
        }

        @DisplayName("userId가 null이면 null을 반환한다.")
        @Test
        void getIsLiked_returnsNull_whenUserIdIsNull() {
            // act
            Boolean result = likeService.getIsLiked(null, productId1);

            // assert
            assertThat(result).isNull();
        }
    }

    @DisplayName("좋아요 여부 일괄 조회")
    @Nested
    class GetIsLikedMap {

        @DisplayName("여러 상품의 좋아요 여부를 일괄 조회할 수 있다.")
        @Test
        void getIsLikedMap() {
            // arrange
            likeService.addLike(userId1, productId1);

            // act
            java.util.Map<Long, Boolean> result = likeService.getIsLikedMap(
                    userId1, java.util.List.of(productId1, productId2)
            );

            // assert
            assertThat(result).hasSize(2);
            assertThat(result.get(productId1)).isTrue();
            assertThat(result.get(productId2)).isFalse();
        }

        @DisplayName("userId가 null이면 빈 맵을 반환한다.")
        @Test
        void getIsLikedMap_returnsEmptyMap_whenUserIdIsNull() {
            // act
            java.util.Map<Long, Boolean> result = likeService.getIsLikedMap(
                    null, java.util.List.of(productId1)
            );

            // assert
            assertThat(result).isEmpty();
        }
    }

    @DisplayName("좋아요 등록 시 likeCount 증가/감소")
    @Nested
    class LikeCountSync {

        @DisplayName("좋아요 등록 시 상품의 likeCount가 증가한다.")
        @Test
        void addLike_increasesLikeCount() {
            // arrange
            Product product = productRepository.findById(productId1).orElseThrow();
            Long beforeCount = product.getLikeCount();

            // act
            likeService.addLike(userId1, productId1);

            // assert
            Product updatedProduct = productRepository.findById(productId1).orElseThrow();
            assertThat(updatedProduct.getLikeCount()).isEqualTo(beforeCount + 1);
        }

        @DisplayName("좋아요 취소 시 상품의 likeCount가 감소한다.")
        @Test
        void removeLike_decreasesLikeCount() {
            // arrange
            likeService.addLike(userId1, productId1);
            Product product = productRepository.findById(productId1).orElseThrow();
            Long afterAddCount = product.getLikeCount();

            // act
            likeService.removeLike(userId1, productId1);

            // assert
            Product updatedProduct = productRepository.findById(productId1).orElseThrow();
            assertThat(updatedProduct.getLikeCount()).isEqualTo(afterAddCount - 1);
        }
    }
}
