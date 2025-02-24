package com.brokerage.event;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResilientEventPublisher {

    private final KafkaTemplate<String, Event> kafkaTemplate;
    private final OutboxService outboxService;

    @Value("${brokerage.order.events.topic}")
    private String orderEventsTopic;

    /**
     * Publishes an order event with circuit breaker and retry patterns
     */
    @CircuitBreaker(name = "kafkaPublisher", fallbackMethod = "fallbackPublish")
    @Retry(name = "kafkaPublishRetry")
    public void publishOrderEvent(Event event) {
        try {
            kafkaTemplate.send(orderEventsTopic, event.getEventId(), event);
            log.info("Published event: {}, type: {}", event.getEventId(), event.getEventType());
        } catch (Exception e) {
            log.error("Failed to publish event: {}", event.getEventId(), e);
            throw e; // Rethrow to trigger retry and circuit breaker
        }
    }

    /**
     * Fallback method when the circuit is open or retries are exhausted
     */
    public void fallbackPublish(Event event, Exception e) {
        log.warn("Circuit open or retries exhausted for event: {}, storing in outbox", event.getEventId());
        outboxService.storeEvent(orderEventsTopic, event);
    }
}