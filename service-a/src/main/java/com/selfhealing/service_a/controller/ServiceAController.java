package com.selfhealing.service_a.controller;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/service-a")
public class ServiceAController {

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        return Map.of(
                "service", "service-a",
                "status", "UP",
                "timestamp", Instant.now().toString());
    }
}
