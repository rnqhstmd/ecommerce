package com.loopers.domain.order;

import java.time.ZonedDateTime;
import java.util.List;

public record OrderPlacedEvent(
        Long orderId,
        String userId,
        Long totalAmount,
        ZonedDateTime paidAt,
        List<OrderItemSnapshot> items
) {
    public record OrderItemSnapshot(
            Long productId,
            String productName,
            Integer quantity,
            Long unitPrice
    ) {}
}
