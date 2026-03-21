package com.loopers.domain.order;

import java.time.ZonedDateTime;
import java.util.List;

public record OrderCancelledEvent(
    Long orderId,
    String userId,
    Long totalAmount,
    ZonedDateTime cancelledAt,
    List<CancelledItem> items
) {
    public record CancelledItem(
        Long productId,
        Integer quantity,
        Long unitPrice
    ) {}

    public static OrderCancelledEvent from(Order order) {
        return new OrderCancelledEvent(
            order.getId(),
            order.getUserId(),
            order.getTotalAmountValue(),
            order.getCancelledAt(),
            order.getOrderItems().stream()
                .map(item -> new CancelledItem(
                    item.getProductId(),
                    item.getQuantity(),
                    item.getUnitPriceValue()
                ))
                .toList()
        );
    }
}
