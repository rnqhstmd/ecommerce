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

    public ProductDetailInfo getProductDetail(Long productId) {
        Product product = productService.getProduct(productId);
        Long likeCount = likeService.getLikeCount(product.getId());
        return ProductDetailInfo.of(product, likeCount);
    }

    public ProductListInfo getProducts(ProductGetListCommand command) {
        ProductSearchCondition condition = new ProductSearchCondition(
                command.brandId(),
                command.pageable()
        );

        Page<Product> productPage = productService.getProducts(condition);
        List<Long> productIds = productPage.getContent().stream()
                .map(Product::getId)
                .toList();
        Map<Long, Long> likeCountMap = likeService.getLikeCountsByProductIds(productIds);
        return ProductListInfo.of(productPage, likeCountMap);
    }
}
