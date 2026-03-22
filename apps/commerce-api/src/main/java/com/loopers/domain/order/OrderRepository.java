package com.loopers.domain.order;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findByIdAndUserId(Long orderId, String userId);
    List<Order> findAllByUserId(String userId);
    Optional<Order> findByIdWithLock(Long orderId);
    Page<Order> findByUserIdAndCondition(OrderSearchCondition condition);
    List<Order> findByUserIdWithCursor(String userId, Long cursor, int size);
}
