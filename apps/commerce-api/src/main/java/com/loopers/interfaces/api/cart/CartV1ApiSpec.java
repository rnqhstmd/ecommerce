package com.loopers.interfaces.api.cart;

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

@Tag(name = "Cart API", description = "장바구니 관리 API")
public interface CartV1ApiSpec {

    @Operation(summary = "장바구니 추가", description = "장바구니에 상품을 추가합니다. 동일 상품은 수량이 합산됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "추가 성공",
                    content = @Content(schema = @Schema(implementation = CartV1Dto.AddItemResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ApiResponse<CartV1Dto.AddItemResponse> addItem(
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader(value = "X-USER-ID", required = false) String userId,
            @Parameter(description = "장바구니 추가 요청", required = true)
            @RequestBody CartV1Dto.AddItemRequest request
    );

    @Operation(summary = "장바구니 삭제", description = "장바구니에서 특정 상품을 제거합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "삭제 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "장바구니에 해당 상품 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ApiResponse<Void> removeItem(
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader(value = "X-USER-ID", required = false) String userId,
            @Parameter(description = "상품 ID", required = true)
            @PathVariable Long productId
    );

    @Operation(summary = "장바구니 조회", description = "장바구니 전체 목록을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = CartV1Dto.CartResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ApiResponse<CartV1Dto.CartResponse> getCart(
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader(value = "X-USER-ID", required = false) String userId
    );

    @Operation(summary = "장바구니 주문 전환", description = "장바구니 전체를 주문으로 전환합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "주문 전환 성공",
                    content = @Content(schema = @Schema(implementation = CartV1Dto.CheckoutResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "장바구니 비어 있음 / 재고 부족 / 포인트 부족",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ApiResponse<CartV1Dto.CheckoutResponse> checkout(
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader(value = "X-USER-ID", required = false) String userId
    );
}
