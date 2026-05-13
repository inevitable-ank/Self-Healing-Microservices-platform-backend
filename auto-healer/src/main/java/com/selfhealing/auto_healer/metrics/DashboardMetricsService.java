package com.selfhealing.auto_healer.metrics;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;

import com.selfhealing.auto_healer.config.PrometheusProperties;

@Service
@ConditionalOnProperty(name = "app.prometheus.enabled", havingValue = "true")
public class DashboardMetricsService {

    private static final Map<String, RangeSpec> RANGE_SPECS = Map.of(
            "15m", new RangeSpec(900, 15),
            "1h", new RangeSpec(3600, 30),
            "6h", new RangeSpec(21_600, 120),
            "24h", new RangeSpec(86_400, 300));

    private final PrometheusRangeClient prometheusRangeClient;
    private final PrometheusProperties prometheusProperties;

    public DashboardMetricsService(PrometheusRangeClient prometheusRangeClient,
            PrometheusProperties prometheusProperties) {
        this.prometheusRangeClient = prometheusRangeClient;
        this.prometheusProperties = prometheusProperties;
    }

    public List<MetricPointResponse> buildSeries(String rangeKey, String serviceIdOrNull) {
        RangeSpec spec = RANGE_SPECS.get(rangeKey);
        if (spec == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported range: " + rangeKey);
        }

        String app = resolveApplication(serviceIdOrNull);
        long end = Instant.now().getEpochSecond();
        long start = end - spec.windowSeconds();

        String cpuQ = "process_cpu_usage{application=\"" + app + "\"}";
        String memQ = "100 * sum(jvm_memory_used_bytes{area=\"heap\",application=\"" + app
                + "\"}) / clamp_min(sum(jvm_memory_max_bytes{area=\"heap\",application=\"" + app + "\"}), 1)";
        String tpQ = "sum(rate(http_server_requests_seconds_count{application=\"" + app + "\"}[2m])) * 60";
        String latQ = "histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{application=\""
                + app + "\"}[5m])) by (le)) * 1000";
        String errQ = "100 * sum(rate(http_server_requests_seconds_count{application=\"" + app
                + "\",status=~\"5..\"}[5m])) / clamp_min(sum(rate(http_server_requests_seconds_count{application=\""
                + app + "\"}[5m])), 1e-9)";

        JsonNode cpuJson = prometheusRangeClient.queryRange(cpuQ, start, end, spec.stepSeconds());
        JsonNode memJson = prometheusRangeClient.queryRange(memQ, start, end, spec.stepSeconds());
        JsonNode tpJson = prometheusRangeClient.queryRange(tpQ, start, end, spec.stepSeconds());
        JsonNode latJson = prometheusRangeClient.queryRange(latQ, start, end, spec.stepSeconds());
        JsonNode errJson = prometheusRangeClient.queryRange(errQ, start, end, spec.stepSeconds());

        NavigableMap<Long, Double> cpu = firstSeriesValues(cpuJson);
        NavigableMap<Long, Double> mem = firstSeriesValues(memJson);
        NavigableMap<Long, Double> tp = firstSeriesValues(tpJson);
        NavigableMap<Long, Double> lat = firstSeriesValues(latJson);
        NavigableMap<Long, Double> err = firstSeriesValues(errJson);

        if (cpu.isEmpty()) {
            return List.of();
        }

        List<MetricPointResponse> out = new ArrayList<>();
        for (Map.Entry<Long, Double> e : cpu.entrySet()) {
            long t = e.getKey();
            double cpuPct = clampPercent(e.getValue() * 100.0);
            double memPct = clampPercent(mem.getOrDefault(t, 0.0));
            double throughput = Math.max(0, tp.getOrDefault(t, 0.0));
            double latency = Math.max(0, lat.getOrDefault(t, 0.0));
            double errorRate = clampPercent(err.getOrDefault(t, 0.0));
            out.add(new MetricPointResponse(
                    Instant.ofEpochSecond(t).toString(),
                    round2(cpuPct),
                    round2(memPct),
                    round2(latency),
                    round2(throughput),
                    round2(errorRate)));
        }
        return Collections.unmodifiableList(out);
    }

    private String resolveApplication(String serviceIdOrNull) {
        String fallback = prometheusProperties.defaultApplication() == null
                ? "service-a"
                : prometheusProperties.defaultApplication();
        if (serviceIdOrNull == null || serviceIdOrNull.isBlank()) {
            return fallback;
        }
        if (!serviceIdOrNull.matches("[a-zA-Z0-9_-]+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid serviceId");
        }
        return serviceIdOrNull;
    }

    private static NavigableMap<Long, Double> firstSeriesValues(JsonNode root) {
        NavigableMap<Long, Double> map = new TreeMap<>();
        JsonNode result = root.path("data").path("result");
        if (!result.isArray() || result.isEmpty()) {
            return map;
        }
        JsonNode values = result.get(0).path("values");
        if (!values.isArray()) {
            return map;
        }
        for (JsonNode pair : values) {
            if (!pair.isArray() || pair.size() < 2) {
                continue;
            }
            long ts = pair.get(0).asLong();
            double v = parseSample(pair.get(1).asText());
            map.put(ts, v);
        }
        return map;
    }

    private static double parseSample(String s) {
        if (s == null) {
            return 0;
        }
        String t = s.trim();
        if (t.isEmpty() || "NaN".equalsIgnoreCase(t) || "+Inf".equalsIgnoreCase(t) || "-Inf".equalsIgnoreCase(t)) {
            return 0;
        }
        try {
            double v = Double.parseDouble(t);
            return Double.isFinite(v) ? v : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static double clampPercent(double v) {
        if (!Double.isFinite(v)) {
            return 0;
        }
        return Math.max(0, Math.min(100, v));
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private record RangeSpec(int windowSeconds, int stepSeconds) {
    }
}
