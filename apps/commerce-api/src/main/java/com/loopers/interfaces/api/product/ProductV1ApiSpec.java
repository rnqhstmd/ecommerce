package com.loopers.interfaces.api.product;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Product API", description = "상품 관리 API")
public interface ProductV1ApiSpec {

    @Operation(summary = "상품 생성", description = "새로운 상품을 등록합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "상품 생성 성공",
                    content = @Content(schema = @Schema(implementation = ProductV1Dto.ProductResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ApiResponse<ProductV1Dto.ProductResponse> createProduct(
            @Parameter(description = "상품 생성 요청 정보", required = true)
            @RequestBody ProductV1Dto.CreateRequest request
    );

    @Operation(summary = "상품 상세 조회", description = "상품 ID로 상품 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ProductV1Dto.ProductResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "상품을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ApiResponse<ProductV1Dto.ProductResponse> getProduct(
            @Parameter(description = "사용자 ID (선택, 로그인 시 isLiked 포함)")
            @RequestHeader(value = "X-USER-ID", required = false) String userId,
            @Parameter(description = "상품 ID", required = true)
            @PathVariable Long productId
    );

    @Operation(summary = "상품 목록 조회", description = "상품 목록을 페이지네이션으로 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ProductV1Dto.ProductListResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ApiResponse<ProductV1Dto.ProductListResponse> getProducts(
            @Parameter(description = "사용자 ID (선택, 로그인 시 isLiked 포함)")
            @RequestHeader(value = "X-USER-ID", required = false) String userId,
            @Parameter(description = "브랜드 ID 필터 (선택)")
            @RequestParam(required = false) Long brandId,
            @Parameter(description = "상품명 키워드 검색 (선택, 최대 100자)")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "최소 가격 필터 (선택, >= 0)")
            @RequestParam(required = false) Long minPrice,
            @Parameter(description = "최대 가격 필터 (선택, >= minPrice)")
            @RequestParam(required = false) Long maxPrice,
            @Parameter(description = "정렬 기준 (latest, price_asc, likes_desc)")
            @RequestParam(defaultValue = "latest") String sort,
            @Parameter(description = "페이지 번호 (0부터 시작)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기 (1~100)")
            @RequestParam(defaultValue = "20") int size
    );
}
