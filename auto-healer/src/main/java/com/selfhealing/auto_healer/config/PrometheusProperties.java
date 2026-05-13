package com.selfhealing.auto_healer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.prometheus")
public record PrometheusProperties(
        boolean enabled,
        String baseUrl,
        String defaultApplication) {
}
