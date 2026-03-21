package com.loopers.infrastructure.order;

import com.loopers.domain.order.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@ConditionalOnBean(KafkaTemplate.class)
@RequiredArgsConstructor
@Slf4j
public class OrderEventPublisher {

    private static final String TOPIC = "order-placed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OrderPlacedEvent event) {
        try {
            kafkaTemplate.send(TOPIC, String.valueOf(event.orderId()), event);
        } catch (Exception e) {
            log.error("Failed to publish OrderPlacedEvent for orderId={}: {}", event.orderId(), e.getMessage(), e);
        }
    }
}
