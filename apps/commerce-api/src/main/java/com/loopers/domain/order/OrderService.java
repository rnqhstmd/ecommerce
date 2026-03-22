package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;

    @Transactional
    public Order save(Order order) {
        return orderRepository.save(order);
    }

    public List<Order> getOrdersByUserId(String userId) {
        return orderRepository.findAllByUserId(userId);
    }

    public Order getOrderByIdAndUserId(Long orderId, String userId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));
    }

    public Order getOrderByIdWithLock(Long orderId) {
        return orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));
    }

    public Page<Order> getOrdersByCondition(OrderSearchCondition condition) {
        return orderRepository.findByUserIdAndCondition(condition);
    }

    public List<Order> getOrdersByUserIdWithCursor(String userId, Long cursor, int size) {
        return orderRepository.findByUserIdWithCursor(userId, cursor, size);
    }
}
