package com.selfhealing.auto_healer.api;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.selfhealing.auto_healer.metrics.DashboardMetricsService;
import com.selfhealing.auto_healer.metrics.MetricPointResponse;

@RestController
@RequestMapping("/api")
@ConditionalOnProperty(name = "app.prometheus.enabled", havingValue = "true")
public class MetricsController {

    private final DashboardMetricsService dashboardMetricsService;

    public MetricsController(DashboardMetricsService dashboardMetricsService) {
        this.dashboardMetricsService = dashboardMetricsService;
    }

    @GetMapping("/metrics")
    public List<MetricPointResponse> metrics(
            @RequestParam(defaultValue = "1h") String range,
            @RequestParam(required = false) String serviceId) {
        List<MetricPointResponse> points = dashboardMetricsService.buildSeries(range, serviceId);
        if (points.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No Prometheus samples for this application (targets up and scraped?)");
        }
        return points;
    }
}
