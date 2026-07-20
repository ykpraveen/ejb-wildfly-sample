package com.example.clinic.api;

import com.example.clinic.api.dto.LoginRequest;
import com.example.clinic.security.JwtService;
import com.example.clinic.user.AuthenticatedUser;
import com.example.clinic.user.UserManagementService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Map;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {
    @Inject
    private UserManagementService userManagementService;

    @Inject
    private JwtService jwtService;

    @POST
    @Path("/login")
    public Map<String, Object> login(@Valid LoginRequest request) {
        AuthenticatedUser user = userManagementService.authenticate(request.clinicId, request.username, request.password);
        String token = jwtService.issueToken(user.username(), user.clinicId(), user.roles());

        return Map.of(
                "accessToken", token,
                "tokenType", "Bearer",
                "clinicId", user.clinicId(),
                "username", user.username(),
                "roles", user.roles()
        );
    }
}
