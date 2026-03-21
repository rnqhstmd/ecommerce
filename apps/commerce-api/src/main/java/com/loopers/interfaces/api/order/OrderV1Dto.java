package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderInfo;
import com.loopers.application.order.OrderPlaceCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.ZonedDateTime;
import java.util.List;

public class OrderV1Dto {

    public record PlaceOrderRequest(
            @NotEmpty(message = "주문 항목은 필수입니다.")
            @Valid
            List<OrderItemRequest> items
    ) {
        public OrderPlaceCommand toCommand(String userId) {
            List<OrderPlaceCommand.OrderItemCommand> itemCommands = items.stream()
                    .map(item -> new OrderPlaceCommand.OrderItemCommand(item.productId(), item.quantity()))
                    .toList();
            return new OrderPlaceCommand(userId, itemCommands);
        }
    }

    public record OrderItemRequest(
            @NotNull(message = "상품 ID는 필수입니다.")
            Long productId,

            @NotNull(message = "수량은 필수입니다.")
            @Positive(message = "수량은 1 이상이어야 합니다.")
            Integer quantity
    ) {}

    public record OrderResponse(
            Long orderId,
            String userId,
            Long totalAmount,
            String status,
            ZonedDateTime paidAt,
            List<OrderItemResponse> items
    ) {
        public static OrderResponse from(OrderInfo info) {
            List<OrderItemResponse> itemResponses = info.items().stream()
                    .map(OrderItemResponse::from)
                    .toList();
            return new OrderResponse(
                    info.orderId(),
                    info.userId(),
                    info.totalAmount(),
                    info.status().name(),
                    info.paidAt(),
                    itemResponses
            );
        }
    }

    public record OrderItemResponse(
            Long productId,
            String productName,
            Integer quantity,
            Long unitPrice,
            Long totalPrice
    ) {
        public static OrderItemResponse from(OrderInfo.OrderItemInfo info) {
            return new OrderItemResponse(
                    info.productId(),
                    info.productName(),
                    info.quantity(),
                    info.unitPrice(),
                    info.totalPrice()
            );
        }
    }

    public record CancelResponse(
            Long orderId,
            String status,
            ZonedDateTime cancelledAt
    ) {
        public static CancelResponse from(OrderInfo.CancelInfo info) {
            return new CancelResponse(
                    info.orderId(),
                    info.status(),
                    info.cancelledAt()
            );
        }
    }

    public record OrderSummaryResponse(
            Long orderId,
            String status,
            Long totalAmount,
            ZonedDateTime paidAt,
            Integer itemCount
    ) {
        public static OrderSummaryResponse from(OrderInfo.OrderSummaryInfo info) {
            return new OrderSummaryResponse(
                    info.orderId(),
                    info.status().name(),
                    info.totalAmount(),
                    info.paidAt(),
                    info.itemCount()
            );
        }
    }
}
