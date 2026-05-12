package com.selfhealing.auto_healer.healing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.selfhealing.auto_healer.event.HealthEvent;

@Service
public class SimulatedRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(SimulatedRecoveryService.class);

    public void onServiceDown(HealthEvent event) {
        log.warn(
                "[SIMULATED] Recovery triggered for service=[{}] eventType=[{}] reason=[{}]. "
                        + "In production: call Kubernetes API (delete pod / rollout restart) or Docker restart for this service.",
                event.getServiceName(),
                event.getEventType(),
                event.getReason());
    }

    public void onServiceRecovered(HealthEvent event) {
        log.info(
                "Service [{}] reported recovered at [{}] (source event from [{}]). No healing action required.",
                event.getServiceName(),
                event.getTimestamp(),
                event.getSource());
    }
}
