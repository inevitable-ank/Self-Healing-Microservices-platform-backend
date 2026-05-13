package com.selfhealing.auto_healer.metrics;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "timestamp", "cpu", "memory", "latencyP95", "throughput", "errorRate" })
public record MetricPointResponse(
        String timestamp,
        double cpu,
        double memory,
        double latencyP95,
        double throughput,
        double errorRate) {
}
