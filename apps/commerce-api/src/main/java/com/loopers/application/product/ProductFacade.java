package com.loopers.application.product;

import com.loopers.domain.like.LikeService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductSearchCondition;
import com.loopers.domain.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductFacade {

    private final ProductService productService;
    private final LikeService likeService;

    @Transactional
    public ProductDetailInfo createProduct(String name, Long price, Integer stock, Long brandId) {
        Product product = productService.createProduct(name, price, stock, brandId);
        return ProductDetailInfo.of(product, 0L);
    }

    public ProductDetailInfo getProductDetail(Long productId, String userId) {
        Product product = productService.getProduct(productId);
        Long likeCount = likeService.getLikeCount(product.getId());
        Boolean isLiked = likeService.getIsLiked(userId, product.getId());
        return ProductDetailInfo.of(product, likeCount, isLiked);
    }

    // 하위 호환 오버로드
    public ProductDetailInfo getProductDetail(Long productId) {
        return getProductDetail(productId, null);
    }

    public ProductListInfo getProducts(ProductGetListCommand command) {
        ProductSearchCondition condition = new ProductSearchCondition(
                command.brandId(),
                command.keyword(),
                command.minPrice(),
                command.maxPrice(),
                command.pageable()
        );

        Page<Product> productPage = productService.getProducts(condition);
        List<Long> productIds = productPage.getContent().stream()
                .map(Product::getId)
                .toList();
        Map<Long, Long> likeCountMap = likeService.getLikeCountsByProductIds(productIds);
        Map<Long, Boolean> isLikedMap = likeService.getIsLikedMap(command.userId(), productIds);
        return ProductListInfo.of(productPage, likeCountMap, isLikedMap);
    }
}
