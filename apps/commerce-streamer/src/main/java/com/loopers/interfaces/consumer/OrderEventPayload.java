package com.loopers.interfaces.consumer;

import java.time.ZonedDateTime;
import java.util.List;

public class OrderEventPayload {

    public record OrderPlacedPayload(
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

    public record OrderCancelledPayload(
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
    }
}
