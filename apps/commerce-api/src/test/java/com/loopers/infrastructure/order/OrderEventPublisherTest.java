package com.loopers.infrastructure.order;

import com.loopers.domain.order.OrderPlacedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventPublisherTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private OrderEventPublisher orderEventPublisher;

    @DisplayName("OrderPlacedEvent 발행 시 order-placed 토픽으로 전송된다.")
    @Test
    void publishOrderPlacedEvent() {
        // arrange
        when(kafkaTemplate.send(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        OrderPlacedEvent event = new OrderPlacedEvent(
                1L,
                "testuser",
                10000L,
                ZonedDateTime.now(),
                List.of(new OrderPlacedEvent.OrderItemSnapshot(100L, "Test Product", 2, 5000L))
        );

        // act
        orderEventPublisher.handle(event);

        // assert
        ArgumentCaptor<Object> valueCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate, times(1)).send(eq("order-placed"), eq("1"), valueCaptor.capture());

        OrderPlacedEvent captured = (OrderPlacedEvent) valueCaptor.getValue();
        assertThat(captured.orderId()).isEqualTo(1L);
        assertThat(captured.userId()).isEqualTo("testuser");
        assertThat(captured.totalAmount()).isEqualTo(10000L);
        assertThat(captured.items()).hasSize(1);
        assertThat(captured.items().get(0).productName()).isEqualTo("Test Product");
    }

    @DisplayName("Kafka 발행 실패 시 예외를 던지지 않고 로그만 남긴다.")
    @Test
    void publishFailureShouldNotThrow() {
        // arrange
        when(kafkaTemplate.send(any(), any(), any()))
                .thenThrow(new RuntimeException("Kafka broker unavailable"));

        OrderPlacedEvent event = new OrderPlacedEvent(
                2L,
                "testuser",
                5000L,
                ZonedDateTime.now(),
                List.of(new OrderPlacedEvent.OrderItemSnapshot(200L, "Another Product", 1, 5000L))
        );

        // act — should not throw
        orderEventPublisher.handle(event);

        // assert
        verify(kafkaTemplate, times(1)).send(eq("order-placed"), eq("2"), any());
    }
}
