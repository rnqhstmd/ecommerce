package com.loopers.domain.order;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderMetricsTest {

    @Test
    void 주문성공_카운터가_증가한다() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OrderMetrics metrics = new OrderMetrics(registry);

        metrics.orderSuccess();
        metrics.orderSuccess();

        assertThat(registry.counter("ecommerce.order.success").count()).isEqualTo(2.0);
    }

    @Test
    void 주문실패_카운터가_증가한다() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OrderMetrics metrics = new OrderMetrics(registry);

        metrics.orderFail();

        assertThat(registry.counter("ecommerce.order.fail").count()).isEqualTo(1.0);
    }

    @Test
    void 재고부족_카운터가_증가한다() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OrderMetrics metrics = new OrderMetrics(registry);

        metrics.stockShortage();

        assertThat(registry.counter("ecommerce.order.stock_shortage").count()).isEqualTo(1.0);
    }
}
