package com.loopers.domain.order;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findByIdAndUserId(Long orderId, String userId);
    List<Order> findAllByUserId(String userId);
}
