package com.loopers.application.order;

import com.loopers.domain.coupon.CouponPolicy;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.coupon.UserCoupon;
import com.loopers.domain.order.*;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.StockDeductionService;
import com.loopers.domain.product.StockDeductionService.StockDeductionCommand;
import com.loopers.interfaces.api.common.PageResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
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
    private final ProductService productService;
    private final CouponService couponService;
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

        // 쿠폰 적용
        long discountAmount = 0L;
        UserCoupon userCoupon = null;
        if (command.couponId() != null) {
            userCoupon = couponService.getUserCouponByIdAndUserId(command.couponId(), command.userId());
            if (userCoupon.isUsed()) {
                throw new CoreException(ErrorType.BAD_REQUEST, "이미 사용된 쿠폰입니다.");
            }
            CouponPolicy policy = couponService.getCouponPolicy(userCoupon.getCouponPolicyId());
            if (!policy.isValid()) {
                throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 유효기간이 아닙니다.");
            }
            discountAmount = policy.calculateDiscount(totalAmount);
        }

        order.applyDiscount(discountAmount, userCoupon != null ? userCoupon.getId() : null);

        long payAmount = totalAmount - discountAmount;
        pointService.usePoint(command.userId(), payAmount);

        if (userCoupon != null) {
            couponService.markCouponUsed(userCoupon);
        }

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

    @Transactional
    public OrderInfo.CancelInfo cancelOrder(Long orderId, String userId) {
        // 1. 비관적 락으로 주문 조회
        Order order = orderService.getOrderByIdWithLock(orderId);

        // 2. 소유자 검증 (소유자 불일치 시 NOT_FOUND)
        if (!order.isOwnedBy(userId)) {
            throw new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다.");
        }

        // 3. 상태 변경 (PAID가 아니면 BAD_REQUEST)
        order.cancel();

        // 4. 재고 복구 (ID 오름차순 비관적 락)
        restoreStock(order);

        // 5. 포인트 환불 — 실제 결제금액(totalAmount - discountAmount)만 환불
        pointService.refundPoint(userId, order.getActualPaymentAmount());

        // 6. 쿠폰 사용 복구
        if (order.getUserCouponId() != null) {
            couponService.restoreCoupon(order.getUserCouponId());
        }

        // 7. 이벤트 발행
        eventPublisher.publishEvent(OrderCancelledEvent.from(order));

        return OrderInfo.CancelInfo.from(order);
    }

    private void restoreStock(Order order) {
        List<OrderItem> sortedItems = order.getOrderItems().stream()
                .sorted(Comparator.comparing(OrderItem::getProductId))
                .toList();

        List<Long> productIds = sortedItems.stream()
                .map(OrderItem::getProductId)
                .distinct()
                .sorted()
                .toList();

        List<Product> products = productService.getProductsByIdsWithLock(productIds);
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        for (OrderItem item : sortedItems) {
            Product product = productMap.get(item.getProductId());
            if (product == null) {
                throw new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다. productId=" + item.getProductId());
            }
            product.increaseStock(item.getQuantity());
        }
    }

    public PageResponse<OrderInfo.OrderSummaryInfo> getMyOrdersPaged(String userId, OrderStatus status, Pageable pageable) {
        OrderSearchCondition condition = new OrderSearchCondition(userId, status, pageable);
        Page<Order> orderPage = orderService.getOrdersByCondition(condition);

        List<OrderInfo.OrderSummaryInfo> content = orderPage.getContent().stream()
                .map(OrderInfo.OrderSummaryInfo::from)
                .toList();

        return PageResponse.of(orderPage, content);
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

    @Transactional(readOnly = true)
    public List<Order> getMyOrdersWithCursor(String userId, Long cursor, int size) {
        return orderService.getOrdersByUserIdWithCursor(userId, cursor, size);
    }
}
