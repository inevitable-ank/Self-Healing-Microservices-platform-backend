package com.selfhealing.auto_healer.metrics;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.selfhealing.auto_healer.config.PrometheusProperties;

/**
 * Calls Prometheus {@code /api/v1/query_range}. PromQL uses braces like {@code {application="x"}}.
 * Spring {@code RestClient} relative URIs apply RFC 6570 template expansion after decoding, which
 * corrupts the {@code query} parameter. {@link HttpClient} with a fully-built {@link URI} avoids that.
 */
@Component
@ConditionalOnProperty(name = "app.prometheus.enabled", havingValue = "true")
public class PrometheusRangeClient {

    private static final Logger log = LoggerFactory.getLogger(PrometheusRangeClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();
    private final ObjectMapper objectMapper;
    private final String prometheusBaseUrl;

    public PrometheusRangeClient(ObjectMapper objectMapper, PrometheusProperties prometheusProperties) {
        this.objectMapper = objectMapper;
        this.prometheusBaseUrl = prometheusProperties.baseUrl().replaceAll("/$", "");
    }

    public JsonNode queryRange(String promql, long startEpochSec, long endEpochSec, int stepSeconds) {
        String q = UriUtils.encodeQueryParam(promql, StandardCharsets.UTF_8);
        String step = UriUtils.encodeQueryParam(stepSeconds + "s", StandardCharsets.UTF_8);
        String uriString = prometheusBaseUrl + "/api/v1/query_range?query=" + q + "&start=" + startEpochSec
                + "&end=" + endEpochSec + "&step=" + step;
        URI uri = URI.create(uriString);

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();

        String body;
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int code = response.statusCode();
            body = response.body();
            if (code != 200) {
                log.warn("Prometheus HTTP {} for query_range: {}", code, abbreviate(body, 400));
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Prometheus HTTP " + code + ": " + abbreviate(body, 240));
            }
        } catch (IOException e) {
            log.warn("Prometheus request failed: {}", e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Cannot reach Prometheus at " + prometheusBaseUrl, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Prometheus query interrupted", e);
        }

        if (body == null || body.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Empty Prometheus response");
        }

        try {
            JsonNode root = objectMapper.readTree(body);
            if (!"success".equals(root.path("status").asText())) {
                String err = root.path("error").asText("unknown error");
                log.warn("Prometheus query failed: {}", err);
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Prometheus: " + err);
            }
            return root;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Failed to parse Prometheus response: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid Prometheus JSON", e);
        }
    }

    private static String abbreviate(String s, int maxChars) {
        if (s == null) {
            return "";
        }
        String t = s.replace('\n', ' ').trim();
        return t.length() <= maxChars ? t : t.substring(0, maxChars) + "…";
    }
}
