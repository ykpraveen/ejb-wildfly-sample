package com.example.clinic.api;

import com.example.clinic.security.JwtPrincipal;
import com.example.clinic.security.JwtService;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class JwtAuthenticationFilter implements ContainerRequestFilter {
    /** Request-scoped property key holding the authenticated principal's clinicId (a Long). */
    public static final String CLINIC_ID_PROPERTY = "clinicId";

    @Inject
    private JwtService jwtService;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String path = requestContext.getUriInfo().getPath();
        if (path.endsWith("auth/login") || path.endsWith("health") || path.endsWith("ready") || path.endsWith("openapi")) {
            return;
        }

        String authHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity(java.util.Map.of("error", "Missing bearer token"))
                    .build());
            return;
        }

        String token = authHeader.substring("Bearer ".length()).trim();
        JwtPrincipal principal;
        try {
            principal = jwtService.verify(token);
        } catch (Exception ex) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity(java.util.Map.of("error", ex.getMessage()))
                    .build());
            return;
        }

        requestContext.setSecurityContext(new TokenSecurityContext(principal, requestContext.getSecurityContext().isSecure()));
        requestContext.setProperty(CLINIC_ID_PROPERTY, principal.clinicId());
    }
}
