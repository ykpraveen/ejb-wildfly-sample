package com.example.clinic.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.time.Instant;
import java.util.Map;

@Path("/ready")
@Produces(MediaType.APPLICATION_JSON)
public class ReadinessResource {
    @GET
    public Map<String, Object> ready() {
        return Map.of(
                "status", "READY",
                "timestamp", Instant.now().toString(),
                "service", "clinic-api",
                "checks", Map.of(
                        "database", "UP",
                        "modules", "loaded"
                )
        );
    }
}
