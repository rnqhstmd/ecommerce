package com.loopers.application.product;

import com.loopers.domain.like.LikeService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductSearchCondition;
import com.loopers.domain.product.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductFacadeTest {

    @Mock
    private ProductService productService;

    @Mock
    private LikeService likeService;

    @InjectMocks
    private ProductFacade productFacade;

    @Nested
    @DisplayName("상품 생성 (createProduct)")
    class CreateProduct {

        @DisplayName("상품을 생성하면 ProductService.createProduct를 호출하고 likeCount 0인 상세 정보를 반환한다.")
        @Test
        void createProduct_delegatesToProductService() {
            // given
            Product product = Product.create("Test Product", 10000L, 50, 1L);
            when(productService.createProduct("Test Product", 10000L, 50, 1L)).thenReturn(product);

            // when
            ProductDetailInfo result = productFacade.createProduct("Test Product", 10000L, 50, 1L);

            // then
            verify(productService, times(1)).createProduct("Test Product", 10000L, 50, 1L);
            assertThat(result.productName()).isEqualTo("Test Product");
            assertThat(result.price()).isEqualTo(10000L);
            assertThat(result.likeCount()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("상품 상세 조회 (getProductDetail)")
    class GetProductDetail {

        @DisplayName("상품 상세 조회 시 product 조회 + 좋아요 수 + isLiked를 조합하여 반환한다.")
        @Test
        void getProductDetail_combinesProductAndLikeInfo() {
            // given
            Product product = org.mockito.Mockito.mock(Product.class);
            when(product.getId()).thenReturn(100L);
            when(product.getName()).thenReturn("Test Product");
            when(product.getPriceValue()).thenReturn(10000L);
            when(product.getStockValue()).thenReturn(50);
            when(product.getBrandId()).thenReturn(1L);

            when(productService.getProduct(100L)).thenReturn(product);
            when(likeService.getLikeCount(100L)).thenReturn(7L);
            when(likeService.getIsLiked("testuser", 100L)).thenReturn(true);

            // when
            ProductDetailInfo result = productFacade.getProductDetail(100L, "testuser");

            // then
            verify(productService, times(1)).getProduct(100L);
            verify(likeService, times(1)).getLikeCount(100L);
            verify(likeService, times(1)).getIsLiked("testuser", 100L);
            assertThat(result.productId()).isEqualTo(100L);
            assertThat(result.likeCount()).isEqualTo(7L);
            assertThat(result.isLiked()).isTrue();
        }
    }

    @Nested
    @DisplayName("상품 목록 조회 (getProducts)")
    class GetProducts {

        @DisplayName("상품 목록 조회 시 페이지 + likeCountMap + isLikedMap을 조합하여 반환한다.")
        @Test
        void getProducts_combinesPageAndLikeMaps() {
            // given
            Product product1 = org.mockito.Mockito.mock(Product.class);
            when(product1.getId()).thenReturn(1L);
            when(product1.getName()).thenReturn("Product 1");
            when(product1.getPriceValue()).thenReturn(1000L);
            when(product1.getBrandId()).thenReturn(10L);

            Product product2 = org.mockito.Mockito.mock(Product.class);
            when(product2.getId()).thenReturn(2L);
            when(product2.getName()).thenReturn("Product 2");
            when(product2.getPriceValue()).thenReturn(2000L);
            when(product2.getBrandId()).thenReturn(10L);

            Pageable pageable = PageRequest.of(0, 10);
            PageImpl<Product> page = new PageImpl<>(List.of(product1, product2), pageable, 2);
            when(productService.getProducts(any(ProductSearchCondition.class))).thenReturn(page);
            when(likeService.getLikeCountsByProductIds(List.of(1L, 2L)))
                    .thenReturn(Map.of(1L, 5L, 2L, 3L));
            when(likeService.getIsLikedMap(eq("testuser"), anyList()))
                    .thenReturn(Map.of(1L, true, 2L, false));

            ProductGetListCommand command = new ProductGetListCommand(10L, "testuser", pageable);

            // when
            ProductListInfo result = productFacade.getProducts(command);

            // then
            verify(productService, times(1)).getProducts(any(ProductSearchCondition.class));
            verify(likeService, times(1)).getLikeCountsByProductIds(List.of(1L, 2L));
            verify(likeService, times(1)).getIsLikedMap(eq("testuser"), anyList());
            assertThat(result.totalElements()).isEqualTo(2);
            assertThat(result.contents()).hasSize(2);
            assertThat(result.contents().get(0).likeCount()).isEqualTo(5L);
            assertThat(result.contents().get(0).isLiked()).isTrue();
            assertThat(result.contents().get(1).likeCount()).isEqualTo(3L);
            assertThat(result.contents().get(1).isLiked()).isFalse();
        }
    }
}
