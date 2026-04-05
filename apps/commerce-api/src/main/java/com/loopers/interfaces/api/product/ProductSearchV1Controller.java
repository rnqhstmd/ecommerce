package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductFacade;
import com.loopers.application.product.ProductFacetInfo;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products/search")
@RequiredArgsConstructor
public class ProductSearchV1Controller implements ProductSearchV1ApiSpec {

    private final ProductFacade productFacade;

    @GetMapping("/autocomplete")
    @Override
    public ApiResponse<ProductSearchV1Dto.AutocompleteResponse> autocomplete(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(defaultValue = "10") int size
    ) {
        if (keyword.isBlank()) {
            return ApiResponse.success(new ProductSearchV1Dto.AutocompleteResponse(List.of()));
        }
        if (size <= 0 || size > 20) {
            throw new CoreException(ErrorType.BAD_REQUEST, "size는 1~20 이어야 합니다.");
        }
        List<String> suggestions = productFacade.autocomplete(keyword.trim(), size);
        return ApiResponse.success(new ProductSearchV1Dto.AutocompleteResponse(suggestions));
    }

    @GetMapping("/facets")
    @Override
    public ApiResponse<ProductSearchV1Dto.FacetResponse> facets(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice
    ) {
        ProductFacetInfo info = productFacade.facets(keyword, minPrice, maxPrice);
        return ApiResponse.success(ProductSearchV1Dto.FacetResponse.from(info));
    }
}
