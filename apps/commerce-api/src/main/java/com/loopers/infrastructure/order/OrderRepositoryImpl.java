package com.loopers.infrastructure.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository jpaRepository;

    @Override
    public Order save(Order order) {
        return jpaRepository.save(order);
    }

    @Override
    public Optional<Order> findByIdAndUserId(Long orderId, String userId) {
        return jpaRepository.findByIdAndUserId(orderId, userId);
    }

    @Override
    public List<Order> findAllByUserId(String userId) {
        return jpaRepository.findAllByUserId(userId);
    }
}
