package com.brokerage.event;

import com.brokerage.repository.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, Event> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxRepository outboxRepository, KafkaTemplate<String, Event> kafkaTemplate, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Store event in outbox for later processing
     */
    @Transactional
    public void storeEvent(String topic, Event event) {
        try {
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventId(event.getEventId())
                    .eventType(event.getEventType())
                    .topic(topic)
                    .payload(objectMapper.writeValueAsString(event))
                    .attempts(0)
                    .processed(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            outboxRepository.save(outboxEvent);
            log.info("Stored event in outbox: {}", event.getEventId());
        } catch (Exception e) {
            log.error("Failed to store event in outbox: {}", event.getEventId(), e);
        }
    }

    /**
     * Process pending events from the outbox
     * Runs every minute
     */
    @Scheduled(fixedDelayString = "${brokerage.outbox.retry-interval:60000}")
    @Transactional
    public void processOutbox() {
        List<OutboxEvent> pendingEvents = outboxRepository.findPendingEvents();
        
        log.info("Processing {} pending events from outbox", pendingEvents.size());
        
        for (OutboxEvent event : pendingEvents) {
            try {
                Event originalEvent = objectMapper.readValue(event.getPayload(),
                        objectMapper.getTypeFactory().constructType(Class.forName(event.getEventType())));
                
                kafkaTemplate.send(event.getTopic(), event.getEventId(), originalEvent);
                
                event.setProcessed(true);
                event.setProcessedAt(LocalDateTime.now());
                outboxRepository.save(event);
                
                log.info("Successfully processed outbox event: {}", event.getEventId());
            } catch (Exception e) {
                log.error("Failed to process outbox event: {}", event.getEventId(), e);
                event.incrementAttempts();
                outboxRepository.save(event);
            }
        }
    }
}
