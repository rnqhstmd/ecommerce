package com.loopers.interfaces.consumer;

import com.loopers.confg.kafka.KafkaConfig;
import com.loopers.domain.notification.Notification;
import com.loopers.domain.notification.NotificationRepository;
import com.loopers.domain.notification.NotificationService;
import com.loopers.domain.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    @KafkaListener(
            topics = "order-placed",
            containerFactory = KafkaConfig.BATCH_LISTENER
    )
    public void handleOrderPlaced(
            List<ConsumerRecord<String, OrderEventPayload.OrderPlacedPayload>> messages,
            Acknowledgment acknowledgment
    ) {
        for (ConsumerRecord<String, OrderEventPayload.OrderPlacedPayload> record : messages) {
            try {
                OrderEventPayload.OrderPlacedPayload payload = record.value();
                Notification notification = Notification.create(
                        payload.userId(),
                        NotificationType.ORDER_PLACED,
                        "주문 완료",
                        String.format("주문(ID: %d)이 완료되었습니다. 결제 금액: %d원", payload.orderId(), payload.totalAmount())
                );
                notificationRepository.save(notification);
                notificationService.send(notification);
            } catch (Exception e) {
                log.error("OrderPlacedEvent 처리 실패: {}", e.getMessage(), e);
            }
        }
        acknowledgment.acknowledge();
    }

    @KafkaListener(
            topics = "order-cancelled",
            containerFactory = KafkaConfig.BATCH_LISTENER
    )
    public void handleOrderCancelled(
            List<ConsumerRecord<String, OrderEventPayload.OrderCancelledPayload>> messages,
            Acknowledgment acknowledgment
    ) {
        for (ConsumerRecord<String, OrderEventPayload.OrderCancelledPayload> record : messages) {
            try {
                OrderEventPayload.OrderCancelledPayload payload = record.value();
                Notification notification = Notification.create(
                        payload.userId(),
                        NotificationType.ORDER_CANCELLED,
                        "주문 취소",
                        String.format("주문(ID: %d)이 취소되었습니다. 환불 금액: %d원", payload.orderId(), payload.totalAmount())
                );
                notificationRepository.save(notification);
                notificationService.send(notification);
            } catch (Exception e) {
                log.error("OrderCancelledEvent 처리 실패: {}", e.getMessage(), e);
            }
        }
        acknowledgment.acknowledge();
    }
}
