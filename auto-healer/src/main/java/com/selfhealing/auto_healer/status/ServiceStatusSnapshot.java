package com.selfhealing.auto_healer.status;

import java.time.Instant;

public record ServiceStatusSnapshot(
        String serviceName,
        String status,
        String lastEventType,
        String reason,
        String source,
        Instant updatedAt) {
}