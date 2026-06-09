package com.loopers.interfaces.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DemoKafkaConsumerTest {

    private final DemoKafkaConsumer demoKafkaConsumer = new DemoKafkaConsumer();

    @Mock
    private Acknowledgment acknowledgment;

    @DisplayName("비어있지 않은 메시지 리스트를 수신하면 acknowledge가 1회 호출된다.")
    @Test
    void acknowledgesWhenMessagesArePresent() {
        // arrange
        List<ConsumerRecord<Object, Object>> messages = List.of(
                new ConsumerRecord<>("demo-topic", 0, 0L, "key1", "value1"),
                new ConsumerRecord<>("demo-topic", 0, 1L, "key2", "value2")
        );

        // act
        demoKafkaConsumer.demoListener(messages, acknowledgment);

        // assert
        verify(acknowledgment, times(1)).acknowledge();
    }

    @DisplayName("빈 메시지 리스트를 수신해도 acknowledge가 1회 호출된다.")
    @Test
    void acknowledgesWhenMessagesAreEmpty() {
        // arrange
        List<ConsumerRecord<Object, Object>> messages = List.of();

        // act
        demoKafkaConsumer.demoListener(messages, acknowledgment);

        // assert
        verify(acknowledgment, times(1)).acknowledge();
    }
}
