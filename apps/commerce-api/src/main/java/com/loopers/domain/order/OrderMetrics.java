package com.loopers.domain.order;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * 주문 핫패스 커스텀 비즈니스 메트릭.
 * 부하테스트 시 /actuator/prometheus 로 노출되어 Grafana 대시보드에서 관측한다.
 */
@Component
public class OrderMetrics {

    private final Counter orderSuccess;
    private final Counter orderFail;
    private final Counter stockShortage;
    private final Timer stockDeductTimer;

    public OrderMetrics(MeterRegistry registry) {
        this.orderSuccess = Counter.builder("ecommerce.order.success")
                .description("주문 성공 건수").register(registry);
        this.orderFail = Counter.builder("ecommerce.order.fail")
                .description("주문 실패 건수").register(registry);
        this.stockShortage = Counter.builder("ecommerce.order.stock_shortage")
                .description("재고 부족으로 거절된 건수").register(registry);
        this.stockDeductTimer = Timer.builder("ecommerce.order.stock_deduct")
                .description("재고 차감 소요 시간").register(registry);
    }

    public void orderSuccess() { orderSuccess.increment(); }
    public void orderFail() { orderFail.increment(); }
    public void stockShortage() { stockShortage.increment(); }
    public Timer stockDeductTimer() { return stockDeductTimer; }
}
