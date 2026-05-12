package com.selfhealing.health_monitor.monitor;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.selfhealing.health_monitor.config.MonitorProperties;
import com.selfhealing.health_monitor.event.HealthEvent;
import com.selfhealing.health_monitor.event.HealthEventPublisher;

@Component
public class ServiceHealthChecker {

    private static final Logger log = LoggerFactory.getLogger(ServiceHealthChecker.class);

    private static final String STATUS_UP = "UP";
    private static final String STATUS_DOWN = "DOWN";
    private static final String STATUS_UNKNOWN = "UNKNOWN";

    private static final String EVENT_SERVICE_DOWN = "SERVICE_DOWN";
    private static final String EVENT_SERVICE_RECOVERED = "SERVICE_RECOVERED";

    private static final String SOURCE = "health-monitor";

    private final RestTemplate restTemplate;
    private final MonitorProperties monitorProperties;
    private final HealthEventPublisher publisher;

    private final AtomicReference<String> lastKnownStatus = new AtomicReference<>(STATUS_UNKNOWN);

    public ServiceHealthChecker(RestTemplate restTemplate,
                                MonitorProperties monitorProperties,
                                HealthEventPublisher publisher) {
        this.restTemplate = restTemplate;
        this.monitorProperties = monitorProperties;
        this.publisher = publisher;
    }

    @Scheduled(fixedDelayString = "${app.monitor.interval-ms:10000}")
    public void check() {
        String url = monitorProperties.getServiceAUrl();
        String serviceName = monitorProperties.getServiceAName();

        String currentStatus;
        String reason = null;

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            Map<?, ?> body = response.getBody();
            Object statusValue = body == null ? null : body.get("status");
            currentStatus = STATUS_UP.equals(String.valueOf(statusValue)) ? STATUS_UP : STATUS_DOWN;
            if (!STATUS_UP.equals(currentStatus)) {
                reason = "Health endpoint returned status=" + statusValue;
            }
        } catch (Exception e) {
            currentStatus = STATUS_DOWN;
            reason = "Health check failed: " + e.getClass().getSimpleName() + " - " + e.getMessage();
        }

        String previousStatus = lastKnownStatus.getAndSet(currentStatus);
        log.debug("Checked [{}] -> previous={}, current={}", serviceName, previousStatus, currentStatus);

        if (STATUS_DOWN.equals(currentStatus) && !STATUS_DOWN.equals(previousStatus)) {
            publisher.publish(buildEvent(EVENT_SERVICE_DOWN, serviceName, currentStatus, reason));
        } else if (STATUS_UP.equals(currentStatus) && STATUS_DOWN.equals(previousStatus)) {
            publisher.publish(buildEvent(EVENT_SERVICE_RECOVERED, serviceName, currentStatus,
                    "Service is reachable and reports UP"));
        }
    }

    private HealthEvent buildEvent(String type, String serviceName, String status, String reason) {
        return new HealthEvent(type, serviceName, status, reason, SOURCE, Instant.now());
    }
}
