package com.selfhealing.health_monitor.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfhealing.health_monitor.config.KafkaTopicProperties;

@Component
public class HealthEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(HealthEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaTopicProperties topicProperties;

    public HealthEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                ObjectMapper objectMapper,
                                KafkaTopicProperties topicProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topicProperties = topicProperties;
    }

    public void publish(HealthEvent event) {
        String topic = topicProperties.getHealthEvents();
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, event.getServiceName(), payload);
            log.info("Published [{}] for service [{}] to topic [{}]",
                    event.getEventType(), event.getServiceName(), topic);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize health event: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to publish health event to Kafka: {}", e.getMessage(), e);
        }
    }
}
