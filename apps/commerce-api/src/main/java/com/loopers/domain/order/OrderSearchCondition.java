package com.loopers.domain.order;

import org.springframework.data.domain.Pageable;

public record OrderSearchCondition(
    String userId,
    OrderStatus status,
    Pageable pageable
) {}
