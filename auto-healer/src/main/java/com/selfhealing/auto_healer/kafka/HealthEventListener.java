package com.selfhealing.auto_healer.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfhealing.auto_healer.event.HealthEvent;
import com.selfhealing.auto_healer.healing.SimulatedRecoveryService;
import com.selfhealing.auto_healer.status.RuntimeStatusStore;

@Component
public class HealthEventListener {

    private static final Logger log = LoggerFactory.getLogger(HealthEventListener.class);

    private static final String SERVICE_DOWN = "SERVICE_DOWN";
    private static final String SERVICE_RECOVERED = "SERVICE_RECOVERED";

    private final ObjectMapper objectMapper;
    private final SimulatedRecoveryService recoveryService;
    private final RuntimeStatusStore runtimeStatusStore;

    public HealthEventListener(
            ObjectMapper objectMapper,
            SimulatedRecoveryService recoveryService,
            RuntimeStatusStore runtimeStatusStore) {
        this.objectMapper = objectMapper;
        this.recoveryService = recoveryService;
        this.runtimeStatusStore = runtimeStatusStore;
    }

    @KafkaListener(
            topics = "${app.kafka.topic.health-events}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void consume(String message) {
        HealthEvent event;
        try {
            event = objectMapper.readValue(message, HealthEvent.class);
        } catch (JsonProcessingException e) {
            log.debug("Skipping non-JSON message (e.g. console test text): {}", summarize(message));
            return;
        }

        if (event.getEventType() == null || event.getServiceName() == null) {
            log.warn("Ignoring health event with missing type or serviceName: {}", message);
            return;
        }

        log.info("Consumed health event: type=[{}] service=[{}] status=[{}]",
                event.getEventType(), event.getServiceName(), event.getStatus());
        runtimeStatusStore.recordEvent(event);

        switch (event.getEventType()) {
            case SERVICE_DOWN -> recoveryService.onServiceDown(event);
            case SERVICE_RECOVERED -> recoveryService.onServiceRecovered(event);
            default -> log.debug("No handler for event type [{}]", event.getEventType());
        }
    }

    private static String summarize(String message) {
        if (message == null) {
            return "null";
        }
        String trimmed = message.trim();
        if (trimmed.length() > 80) {
            return trimmed.substring(0, 80) + "...";
        }
        return trimmed;
    }
}