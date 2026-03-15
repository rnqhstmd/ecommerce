package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderPlacedEvent;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.StockDeductionService;
import com.loopers.domain.product.StockDeductionService.StockDeductionCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderFacade {

    private final OrderService orderService;
    private final StockDeductionService stockDeductionService;
    private final PointService pointService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public OrderInfo placeOrder(OrderPlaceCommand command) {
        List<StockDeductionCommand> deductionCommands = command.items().stream()
                .map(item -> new StockDeductionCommand(item.productId(), item.quantity()))
                .toList();

        List<Product> products = stockDeductionService.deductStock(deductionCommands);
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        Order order = Order.create(command.userId());

        for (OrderPlaceCommand.OrderItemCommand item : command.items()) {
            Product product = productMap.get(item.productId());
            order.addOrderItem(product.getId(), product.getName(), product.getPriceValue(), item.quantity());
        }

        Long totalAmount = order.getTotalAmountValue();
        pointService.usePoint(command.userId(), totalAmount);

        order.completePayment();

        Order savedOrder = orderService.save(order);

        OrderPlacedEvent event = new OrderPlacedEvent(
                savedOrder.getId(),
                savedOrder.getUserId(),
                savedOrder.getTotalAmountValue(),
                savedOrder.getPaidAt(),
                savedOrder.getOrderItems().stream()
                        .map(item -> new OrderPlacedEvent.OrderItemSnapshot(
                                item.getProductId(),
                                item.getProductName(),
                                item.getQuantity(),
                                item.getUnitPriceValue()
                        ))
                        .toList()
        );
        eventPublisher.publishEvent(event);

        return OrderInfo.from(savedOrder);
    }

    public List<OrderInfo> getMyOrders(String userId) {
        List<Order> orders = orderService.getOrdersByUserId(userId);

        return orders.stream()
                .map(OrderInfo::from)
                .toList();
    }

    public OrderInfo getOrderDetail(Long orderId, String userId) {
        Order order = orderService.getOrderByIdAndUserId(orderId, userId);

        return OrderInfo.from(order);
    }
}
