package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductDetailInfo;
import com.loopers.application.product.ProductFacade;
import com.loopers.application.product.ProductGetListCommand;
import com.loopers.application.product.ProductListInfo;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
            @RequestHeader(value = "X-USER-ID", required = false) String userId,
            @PathVariable Long productId
    ) {
        ProductDetailInfo info = productFacade.getProductDetail(productId, userId);
        return ApiResponse.success(ProductV1Dto.ProductResponse.from(info));
    }

    @GetMapping
    @Override
    public ApiResponse<ProductV1Dto.ProductListResponse> getProducts(
            @RequestHeader(value = "X-USER-ID", required = false) String userId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (page < 0 || size <= 0 || size > 100) {
            throw new CoreException(ErrorType.BAD_REQUEST, "page는 0 이상, size는 1~100 이어야 합니다.");
        }
        Sort sortOrder = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortOrder);
        ProductGetListCommand command = new ProductGetListCommand(brandId, userId, pageable);
        ProductListInfo info = productFacade.getProducts(command);
        return ApiResponse.success(ProductV1Dto.ProductListResponse.from(info));
    }

    private Sort parseSort(String sort) {
        return switch (sort) {
            case "latest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price.value");
            case "likes_desc" -> Sort.by(Sort.Direction.DESC, "likeCount");
            default -> throw new CoreException(ErrorType.BAD_REQUEST,
                    "sort는 latest, price_asc, likes_desc 중 하나여야 합니다.");
        };
    }
}
