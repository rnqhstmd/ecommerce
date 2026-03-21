package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.order.OrderItem;

import java.time.ZonedDateTime;
import java.util.List;

public record OrderInfo(
        Long orderId,
        String userId,
        Long totalAmount,
        OrderStatus status,
        ZonedDateTime paidAt,
        List<OrderItemInfo> items
) {
    public record OrderItemInfo(
            Long productId,
            String productName,
            Integer quantity,
            Long unitPrice,
            Long totalPrice
    ) {
        public static OrderItemInfo from(OrderItem item) {
            return new OrderItemInfo(
                    item.getProductId(),
                    item.getProductName(),
                    item.getQuantity(),
                    item.getUnitPriceValue(),
                    item.calculateAmount().getValue()
            );
        }
    }

    public static OrderInfo from(Order order) {
        return new OrderInfo(
                order.getId(),
                order.getUserId(),
                order.getTotalAmountValue(),
                order.getStatus(),
                order.getPaidAt(),
                order.getOrderItems().stream()
                        .map(OrderItemInfo::from)
                        .toList()
        );
    }

    public record OrderSummaryInfo(
            Long orderId,
            OrderStatus status,
            Long totalAmount,
            ZonedDateTime paidAt,
            Integer itemCount
    ) {
        public static OrderSummaryInfo from(Order order) {
            return new OrderSummaryInfo(
                    order.getId(),
                    order.getStatus(),
                    order.getTotalAmountValue(),
                    order.getPaidAt(),
                    order.getOrderItems().size()
            );
        }
    }

    public record CancelInfo(
            Long orderId,
            String status,
            ZonedDateTime cancelledAt
    ) {
        public static CancelInfo from(Order order) {
            return new CancelInfo(
                    order.getId(),
                    order.getStatus().name(),
                    order.getCancelledAt()
            );
        }
    }
}
