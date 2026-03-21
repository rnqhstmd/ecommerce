package com.loopers.interfaces.api.review;

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

@Tag(name = "Review API", description = "리뷰 관리 API")
public interface ReviewV1ApiSpec {

    @Operation(summary = "리뷰 작성", description = "결제 완료된 주문에 대해 리뷰를 작성합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "리뷰 작성 성공",
                    content = @Content(schema = @Schema(implementation = ReviewV1Dto.ReviewResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 (X-USER-ID 누락)",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ApiResponse<ReviewV1Dto.ReviewResponse> createReview(
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader(value = "X-USER-ID", required = false) String userId,
            @Parameter(description = "리뷰 작성 요청 정보", required = true)
            @RequestBody ReviewV1Dto.CreateReviewRequest request
    );

    @Operation(summary = "상품 리뷰 조회", description = "상품의 리뷰 목록을 페이징하여 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ReviewV1Dto.ProductReviewResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ApiResponse<ReviewV1Dto.ProductReviewResponse> getProductReviews(
            @Parameter(description = "상품 ID", required = true)
            @PathVariable Long productId,
            @Parameter(description = "페이지 번호 (0부터 시작)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기 (1~100)")
            @RequestParam(defaultValue = "20") int size
    );
}
