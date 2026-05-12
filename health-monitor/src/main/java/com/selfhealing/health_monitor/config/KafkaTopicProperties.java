package com.selfhealing.health_monitor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka.topic")
public class KafkaTopicProperties {

    private String healthEvents;

    public String getHealthEvents() {
        return healthEvents;
    }

    public void setHealthEvents(String healthEvents) {
        this.healthEvents = healthEvents;
    }
}
