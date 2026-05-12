package com.selfhealing.auto_healer.api;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.selfhealing.auto_healer.event.HealthEvent;
import com.selfhealing.auto_healer.status.RuntimeStatusStore;
import com.selfhealing.auto_healer.status.ServiceStatusSnapshot;

@Validated
@RestController
@RequestMapping("/api/status")
public class StatusController {

    private final RuntimeStatusStore runtimeStatusStore;

    public StatusController(RuntimeStatusStore runtimeStatusStore) {
        this.runtimeStatusStore = runtimeStatusStore;
    }

    @GetMapping("/services")
    public List<ServiceStatusSnapshot> listServices() {
        return runtimeStatusStore.listServiceStatuses();
    }

    @GetMapping("/services/{serviceName}")
    public ServiceStatusSnapshot getService(@PathVariable String serviceName) {
        return runtimeStatusStore.findServiceStatus(serviceName)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No status tracked for service '" + serviceName + "'"));
    }

    @GetMapping("/events/recent")
    public List<HealthEvent> getRecentEvents(@RequestParam(defaultValue = "20") @Min(1) @Max(500) int limit) {
        return runtimeStatusStore.getRecentEvents(limit);
    }
}