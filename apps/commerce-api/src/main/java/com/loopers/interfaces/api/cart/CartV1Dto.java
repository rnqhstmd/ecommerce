package com.loopers.interfaces.api.cart;

import com.loopers.application.cart.CartItemInfo;
import com.loopers.application.order.OrderInfo;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.ZonedDateTime;
import java.util.List;

public class CartV1Dto {

    public record AddItemRequest(
            @NotNull(message = "상품 ID는 필수입니다.")
            Long productId,

            @NotNull(message = "수량은 필수입니다.")
            @Positive(message = "수량은 1 이상이어야 합니다.")
            Integer quantity
    ) {}

    public record AddItemResponse(
            Long productId,
            long quantity
    ) {}

    public record CartResponse(
            List<CartItemResponse> items
    ) {
        public static CartResponse from(List<CartItemInfo> items) {
            List<CartItemResponse> responses = items.stream()
                    .map(CartItemResponse::from)
                    .toList();
            return new CartResponse(responses);
        }
    }

    public record CartItemResponse(
            Long productId,
            String productName,
            Long price,
            int quantity,
            String stockStatus
    ) {
        public static CartItemResponse from(CartItemInfo info) {
            return new CartItemResponse(
                    info.productId(),
                    info.productName(),
                    info.price(),
                    info.quantity(),
                    info.stockStatus()
            );
        }
    }

    public record CheckoutResponse(
            Long orderId,
            String userId,
            Long totalAmount,
            String status,
            ZonedDateTime paidAt,
            List<CheckoutItemResponse> items
    ) {
        public static CheckoutResponse from(OrderInfo info) {
            List<CheckoutItemResponse> itemResponses = info.items().stream()
                    .map(CheckoutItemResponse::from)
                    .toList();
            return new CheckoutResponse(
                    info.orderId(),
                    info.userId(),
                    info.totalAmount(),
                    info.status().name(),
                    info.paidAt(),
                    itemResponses
            );
        }
    }

    public record CheckoutItemResponse(
            Long productId,
            String productName,
            Integer quantity,
            Long unitPrice,
            Long totalPrice
    ) {
        public static CheckoutItemResponse from(OrderInfo.OrderItemInfo info) {
            return new CheckoutItemResponse(
                    info.productId(),
                    info.productName(),
                    info.quantity(),
                    info.unitPrice(),
                    info.totalPrice()
            );
        }
    }
}
