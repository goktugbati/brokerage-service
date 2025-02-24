package com.brokerage.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventPublisher {

    private final KafkaTemplate<String, Event> kafkaTemplate;

    @Value("${brokerage.order.events.topic}")
    private String orderEventsTopic;

    public void publishOrderEvent(Event event) {
        try {
            kafkaTemplate.send(orderEventsTopic, event.getEventId(), event);
            log.info("Published event: {}, type: {}", event.getEventId(), event.getEventType());
        } catch (Exception e) {
            log.error("Failed to publish event: {}", event.getEventId(), e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }
}
