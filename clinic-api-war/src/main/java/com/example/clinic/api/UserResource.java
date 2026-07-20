package com.example.clinic.api;

import com.example.clinic.api.dto.ActivateUserRequest;
import com.example.clinic.api.dto.CreateUserRequest;
import com.example.clinic.user.UserAccount;
import com.example.clinic.user.UserManagementService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;
import java.util.Map;

@Path("/users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {
    @Inject
    private UserManagementService userManagementService;

    @POST
    public Map<String, Object> createUser(@Valid CreateUserRequest request, @Context SecurityContext securityContext) {
        assertAdmin(securityContext);

        UserAccount user = userManagementService.createUser(
                request.clinicId,
                request.username,
                request.password,
                request.roles
        );
        return toPayload(user);
    }

    @PATCH
    @Path("/{userId}/activate")
    public Map<String, Object> activateUser(
            @PathParam("userId") Long userId,
            @Valid ActivateUserRequest request,
            @Context SecurityContext securityContext
    ) {
        assertAdmin(securityContext);
        if (request == null || request.clinicId == null) {
            throw new BadRequestException("clinicId is required");
        }

        UserAccount user = userManagementService.activateUser(request.clinicId, userId);
        return toPayload(user);
    }

    @GET
    public List<Map<String, Object>> listUsers(
            @QueryParam("clinicId") Long clinicId,
            @Context SecurityContext securityContext
    ) {
        assertAdmin(securityContext);
        if (clinicId == null) {
            throw new BadRequestException("clinicId query param is required");
        }
        return userManagementService.listUsers(clinicId).stream().map(this::toPayload).toList();
    }

    private void assertAdmin(SecurityContext securityContext) {
        if (securityContext.getUserPrincipal() == null || !securityContext.isUserInRole("ADMIN")) {
            throw new ForbiddenException("ADMIN role is required");
        }
    }

    private Map<String, Object> toPayload(UserAccount user) {
        return Map.of(
                "id", user.getId(),
                "clinicId", user.getClinicId(),
                "username", user.getUsername(),
                "roles", user.getRoles().stream().map(Enum::name).toList(),
                "active", user.isActive(),
                "deleted", user.isDeleted(),
                "createdAt", user.getCreatedAt().toString(),
                "updatedAt", user.getUpdatedAt().toString()
        );
    }
}
