package com.selfhealing.health_monitor.event;

import java.time.Instant;

public class HealthEvent {

    private String eventType;
    private String serviceName;
    private String status;
    private String reason;
    private String source;
    private Instant timestamp;

    public HealthEvent() {
    }

    public HealthEvent(String eventType, String serviceName, String status,
                       String reason, String source, Instant timestamp) {
        this.eventType = eventType;
        this.serviceName = serviceName;
        this.status = status;
        this.reason = reason;
        this.source = source;
        this.timestamp = timestamp;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
