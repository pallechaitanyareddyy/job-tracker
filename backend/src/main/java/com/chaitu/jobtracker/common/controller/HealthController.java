package com.chaitu.jobtracker.common.controller;

import java.time.Instant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A tiny public endpoint the React app calls to prove the two sides are connected.
 * (Spring Boot Actuator also exposes /actuator/health, which checks the database.)
 */
@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public HealthResponse health() {
        return new HealthResponse("UP", "job-tracker-api", Instant.now());
    }

    public record HealthResponse(String status, String service, Instant time) {
    }
}
