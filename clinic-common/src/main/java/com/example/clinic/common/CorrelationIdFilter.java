package com.example.clinic.common;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.util.UUID;

@Provider
@Priority(Priorities.HEADER_DECORATOR)
public class CorrelationIdFilter implements ContainerRequestFilter, ContainerResponseFilter {
    public static final String HEADER = "X-Correlation-Id";
    public static final String PROP = "correlationId";

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    public static String current() {
        String id = CURRENT.get();
        return id != null ? id : UUID.randomUUID().toString();
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String incoming = requestContext.getHeaderString(HEADER);
        String correlationId = (incoming == null || incoming.isBlank()) ? UUID.randomUUID().toString() : incoming;
        requestContext.setProperty(PROP, correlationId);
        CURRENT.set(correlationId);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        Object correlationId = requestContext.getProperty(PROP);
        if (correlationId != null) {
            responseContext.getHeaders().putSingle(HEADER, correlationId.toString());
        }
        CURRENT.remove();
    }
}
