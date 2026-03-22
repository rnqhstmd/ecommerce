package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderInfo;
import com.loopers.application.order.OrderPlaceCommand;
import com.loopers.domain.order.OrderStatus;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.common.CursorPageRequest;
import com.loopers.interfaces.api.common.CursorPageResponse;
import com.loopers.interfaces.api.common.PageResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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
        validateUserId(userId);

        OrderPlaceCommand command = request.toCommand(userId);
        OrderInfo orderInfo = orderFacade.placeOrder(command);
        return ApiResponse.success(OrderV1Dto.OrderResponse.from(orderInfo));
    }

    @PostMapping("/{id}/cancel")
    @Override
    public ApiResponse<OrderV1Dto.CancelResponse> cancelOrder(
            @PathVariable Long id,
            @RequestHeader(value = "X-USER-ID", required = false) String userId
    ) {
        validateUserId(userId);
        OrderInfo.CancelInfo info = orderFacade.cancelOrder(id, userId);
        return ApiResponse.success(OrderV1Dto.CancelResponse.from(info));
    }

    @GetMapping
    @Override
    public ApiResponse<PageResponse<OrderV1Dto.OrderSummaryResponse>> getOrders(
            @RequestHeader(value = "X-USER-ID", required = false) String userId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        validateUserId(userId);
        if (page < 0 || size < 1 || size > 100) {
            throw new CoreException(ErrorType.BAD_REQUEST, "page는 0 이상, size는 1 이상 100 이하여야 합니다.");
        }

        OrderStatus orderStatus = parseOrderStatus(status);
        PageResponse<OrderInfo.OrderSummaryInfo> pageResponse =
                orderFacade.getMyOrdersPaged(userId, orderStatus, PageRequest.of(page, size));

        return ApiResponse.success(pageResponse.map(OrderV1Dto.OrderSummaryResponse::from));
    }

    @GetMapping("/cursor")
    @Override
    public ApiResponse<CursorPageResponse<OrderV1Dto.OrderSummaryResponse>> getOrdersWithCursor(
            @RequestHeader(value = "X-USER-ID", required = false) String userId,
            @Valid @ModelAttribute CursorPageRequest cursorPageRequest
    ) {
        validateUserId(userId);

        List<OrderInfo.OrderSummaryInfo> summaries = orderFacade.getMyOrdersWithCursor(userId, cursorPageRequest.cursor(), cursorPageRequest.size());
        CursorPageResponse<OrderV1Dto.OrderSummaryResponse> response = CursorPageResponse.of(
                summaries, cursorPageRequest.size(),
                OrderInfo.OrderSummaryInfo::orderId,
                OrderV1Dto.OrderSummaryResponse::from
        );
        return ApiResponse.success(response);
    }

    @GetMapping("/{id}")
    @Override
    public ApiResponse<OrderV1Dto.OrderResponse> getOrderDetail(
            @RequestHeader(value = "X-USER-ID", required = false) String userId,
            @PathVariable Long id
    ) {
        validateUserId(userId);
        OrderInfo orderInfo = orderFacade.getOrderDetail(id, userId);
        return ApiResponse.success(OrderV1Dto.OrderResponse.from(orderInfo));
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new CoreException(ErrorType.UNAUTHORIZED, "X-USER-ID 헤더는 필수입니다.");
        }
    }

    private OrderStatus parseOrderStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return OrderStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                    String.format("유효하지 않은 주문 상태입니다: '%s'. 사용 가능한 값: PENDING, PAID, CANCELLED", status));
        }
    }
}
