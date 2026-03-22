package com.loopers.infrastructure.order;

import com.loopers.domain.order.OrderCancelledEvent;
import com.loopers.domain.order.OrderPlacedEvent;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@ConditionalOnBean(KafkaTemplate.class)
@RequiredArgsConstructor
@Slf4j
public class OrderEventPublisher {

    private static final String TOPIC_ORDER_PLACED = "order-placed";
    private static final String TOPIC_ORDER_CANCELLED = "order-cancelled";
    private static final long SEND_TIMEOUT_SECONDS = 5;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @CircuitBreaker(name = "kafkaPublisher", fallbackMethod = "handleOrderPlacedFallback")
    public void handle(OrderPlacedEvent event) {
        try {
            kafkaTemplate.send(TOPIC_ORDER_PLACED, String.valueOf(event.orderId()), event)
                    .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Kafka send interrupted: orderId=" + event.orderId(), e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Kafka send failed: orderId=" + event.orderId(), e.getCause());
        } catch (TimeoutException e) {
            throw new RuntimeException("Kafka send timed out: orderId=" + event.orderId(), e);
        }
        log.info("OrderPlacedEvent 발행 성공: orderId={}", event.orderId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @CircuitBreaker(name = "kafkaPublisher", fallbackMethod = "handleOrderCancelledFallback")
    public void handleOrderCancelledEvent(OrderCancelledEvent event) {
        try {
            kafkaTemplate.send(TOPIC_ORDER_CANCELLED, String.valueOf(event.orderId()), event)
                    .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Kafka send interrupted: orderId=" + event.orderId(), e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Kafka send failed: orderId=" + event.orderId(), e.getCause());
        } catch (TimeoutException e) {
            throw new RuntimeException("Kafka send timed out: orderId=" + event.orderId(), e);
        }
        log.info("OrderCancelledEvent 발행 성공: orderId={}", event.orderId());
    }

    private void handleOrderPlacedFallback(OrderPlacedEvent event, Throwable t) {
        log.error("OrderPlacedEvent Kafka 발행 실패 (CircuitBreaker fallback): orderId={}, error={}",
                event.orderId(), t.getMessage(), t);
    }

    private void handleOrderCancelledFallback(OrderCancelledEvent event, Throwable t) {
        log.error("OrderCancelledEvent Kafka 발행 실패 (CircuitBreaker fallback): orderId={}, error={}",
                event.orderId(), t.getMessage(), t);
    }
}
