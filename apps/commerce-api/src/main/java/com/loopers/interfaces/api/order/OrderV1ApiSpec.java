package com.loopers.interfaces.api.order;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Order API", description = "주문 관리 API")
public interface OrderV1ApiSpec {

    @Operation(summary = "주문 생성", description = "새로운 주문을 생성합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "주문 생성 성공",
                    content = @Content(schema = @Schema(implementation = OrderV1Dto.OrderResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    ApiResponse<OrderV1Dto.OrderResponse> placeOrder(
            @Parameter(description = "사용자 ID", required = true)
            @RequestHeader(value = "X-USER-ID", required = false) String userId,
            @Parameter(description = "주문 요청 정보", required = true)
            @RequestBody OrderV1Dto.PlaceOrderRequest request
    );
}
