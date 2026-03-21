package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderInfo;
import com.loopers.application.order.OrderPlaceCommand;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderV1Controller implements OrderV1ApiSpec {

    private final OrderFacade orderFacade;

    @PostMapping
    @Override
    public ApiResponse<OrderV1Dto.OrderResponse> placeOrder(
            @RequestHeader(value = "X-USER-ID", required = false) String userId,
            @RequestBody @Valid OrderV1Dto.PlaceOrderRequest request
    ) {
        if (userId == null || userId.isBlank()) {
            throw new CoreException(ErrorType.UNAUTHORIZED, "X-USER-ID 헤더는 필수입니다.");
        }

        OrderPlaceCommand command = request.toCommand(userId);
        OrderInfo orderInfo = orderFacade.placeOrder(command);
        return ApiResponse.success(OrderV1Dto.OrderResponse.from(orderInfo));
    }

    @GetMapping
    @Override
    public ApiResponse<List<OrderV1Dto.OrderResponse>> getMyOrders(
            @RequestHeader(value = "X-USER-ID", required = false) String userId
    ) {
        if (userId == null || userId.isBlank()) {
            throw new CoreException(ErrorType.UNAUTHORIZED, "X-USER-ID 헤더는 필수입니다.");
        }
        List<OrderInfo> orders = orderFacade.getMyOrders(userId);
        List<OrderV1Dto.OrderResponse> responses = orders.stream()
                .map(OrderV1Dto.OrderResponse::from).toList();
        return ApiResponse.success(responses);
    }

    @GetMapping("/{id}")
    @Override
    public ApiResponse<OrderV1Dto.OrderResponse> getOrderDetail(
            @RequestHeader(value = "X-USER-ID", required = false) String userId,
            @PathVariable Long id
    ) {
        if (userId == null || userId.isBlank()) {
            throw new CoreException(ErrorType.UNAUTHORIZED, "X-USER-ID 헤더는 필수입니다.");
        }
        OrderInfo orderInfo = orderFacade.getOrderDetail(id, userId);
        return ApiResponse.success(OrderV1Dto.OrderResponse.from(orderInfo));
    }
}
