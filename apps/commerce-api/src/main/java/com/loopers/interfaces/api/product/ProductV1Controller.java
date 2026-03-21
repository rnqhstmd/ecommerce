package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductDetailInfo;
import com.loopers.application.product.ProductFacade;
import com.loopers.interfaces.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductV1Controller implements ProductV1ApiSpec {

    private final ProductFacade productFacade;

    @PostMapping
    @Override
    public ApiResponse<ProductV1Dto.ProductResponse> createProduct(
            @RequestBody @Valid ProductV1Dto.CreateRequest request
    ) {
        ProductDetailInfo info = productFacade.createProduct(
                request.name(), request.price(), request.stock(), request.brandId()
        );
        return ApiResponse.success(ProductV1Dto.ProductResponse.from(info));
    }

    @GetMapping("/{productId}")
    @Override
    public ApiResponse<ProductV1Dto.ProductResponse> getProduct(
            @PathVariable Long productId
    ) {
        ProductDetailInfo info = productFacade.getProductDetail(productId);
        return ApiResponse.success(ProductV1Dto.ProductResponse.from(info));
    }
}
