package com.loopers.interfaces.api.product;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Product Search API", description = "상품 검색 API (자동완성, 집계)")
public interface ProductSearchV1ApiSpec {

    @Operation(summary = "자동완성", description = "상품명 접두사로 자동완성 결과를 반환합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    ApiResponse<ProductSearchV1Dto.AutocompleteResponse> autocomplete(
            @Parameter(description = "검색 키워드 (접두사)") @RequestParam(required = false, defaultValue = "") String keyword,
            @Parameter(description = "결과 개수 (1~20, 기본 10)") @RequestParam(defaultValue = "10") int size
    );

    @Operation(summary = "검색 집계", description = "브랜드/카테고리/가격대별 상품 수 집계를 반환합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ApiResponse<ProductSearchV1Dto.FacetResponse> facets(
            @Parameter(description = "검색 키워드 (선택)") @RequestParam(required = false) String keyword,
            @Parameter(description = "최소 가격 (선택)") @RequestParam(required = false) Long minPrice,
            @Parameter(description = "최대 가격 (선택)") @RequestParam(required = false) Long maxPrice
    );
}
