package com.selfhealing.auto_healer.status;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.selfhealing.auto_healer.event.HealthEvent;

@Service
public class RuntimeStatusStore {

    private static final String STATUS_UP = "UP";
    private static final String STATUS_DOWN = "DOWN";

    private final ConcurrentMap<String, ServiceStatusSnapshot> serviceStatusByName = new ConcurrentHashMap<>();
    private final Deque<HealthEvent> recentEvents = new ArrayDeque<>();
    private final ReentrantLock recentEventsLock = new ReentrantLock();
    private final int recentEventsLimit;

    public RuntimeStatusStore(@Value("${app.status.recent-events-limit:200}") int recentEventsLimit) {
        this.recentEventsLimit = Math.max(1, recentEventsLimit);
    }

    public void recordEvent(HealthEvent event) {
        Instant updatedAt = event.getTimestamp() == null ? Instant.now() : event.getTimestamp();
        String status = normalizeStatus(event);

        ServiceStatusSnapshot snapshot = new ServiceStatusSnapshot(
                event.getServiceName(),
                status,
                event.getEventType(),
                event.getReason(),
                event.getSource(),
                updatedAt);

        serviceStatusByName.put(event.getServiceName(), snapshot);
        appendRecentEvent(copyEvent(event, updatedAt, status));
    }

    public List<ServiceStatusSnapshot> listServiceStatuses() {
        return serviceStatusByName.values().stream()
                .sorted(Comparator.comparing(ServiceStatusSnapshot::serviceName))
                .toList();
    }

    public Optional<ServiceStatusSnapshot> findServiceStatus(String serviceName) {
        return Optional.ofNullable(serviceStatusByName.get(serviceName));
    }

    public List<HealthEvent> getRecentEvents(int limit) {
        int boundedLimit = Math.max(1, limit);
        recentEventsLock.lock();
        try {
            List<HealthEvent> snapshot = new ArrayList<>(recentEvents);
            snapshot.sort(Comparator
                    .comparing(HealthEvent::getTimestamp, Comparator.nullsLast(Comparator.naturalOrder()))
                    .reversed());
            if (snapshot.size() <= boundedLimit) {
                return snapshot;
            }
            return snapshot.subList(0, boundedLimit);
        } finally {
            recentEventsLock.unlock();
        }
    }

    private void appendRecentEvent(HealthEvent event) {
        recentEventsLock.lock();
        try {
            recentEvents.addLast(event);
            while (recentEvents.size() > recentEventsLimit) {
                recentEvents.removeFirst();
            }
        } finally {
            recentEventsLock.unlock();
        }
    }

    private static String normalizeStatus(HealthEvent event) {
        if (event.getStatus() != null && !event.getStatus().isBlank()) {
            return event.getStatus().trim().toUpperCase();
        }
        if ("SERVICE_DOWN".equals(event.getEventType())) {
            return STATUS_DOWN;
        }
        if ("SERVICE_RECOVERED".equals(event.getEventType())) {
            return STATUS_UP;
        }
        return "UNKNOWN";
    }

    private static HealthEvent copyEvent(HealthEvent original, Instant timestamp, String status) {
        HealthEvent copy = new HealthEvent();
        copy.setEventType(original.getEventType());
        copy.setServiceName(original.getServiceName());
        copy.setStatus(status);
        copy.setReason(original.getReason());
        copy.setSource(original.getSource());
        copy.setTimestamp(timestamp);
        return copy;
    }
}