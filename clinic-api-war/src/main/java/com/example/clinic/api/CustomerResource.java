package com.example.clinic.api;

import com.example.clinic.api.dto.CreateCustomerRequest;
import com.example.clinic.api.dto.UpdateCustomerRequest;
import com.example.clinic.audit.AuditService;
import com.example.clinic.common.CorrelationIdFilter;
import com.example.clinic.customer.Customer;
import com.example.clinic.customer.CustomerManagementService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/customers")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CustomerResource {
    private static final Logger LOG = Logger.getLogger(CustomerResource.class.getName());

    @Inject
    private CustomerManagementService customerManagementService;

    @Inject
    private AuditService auditService;

    @POST
    @RolesAllowed({"ADMIN", "USER"})
    public Response createCustomer(
            @Valid CreateCustomerRequest request,
            @Context SecurityContext securityContext,
            @Context HttpServletRequest httpRequest
    ) {
        TenantGuard.requireClinic(httpRequest, request.clinicId);
        Customer customer = customerManagementService.createCustomer(
                request.clinicId,
                request.fullName,
                request.username,
                request.email,
                request.phone
        );
        recordAudit(request.clinicId, securityContext, "CUSTOMER_CREATED", customer.getId(),
                "username=" + customer.getUsername());
        return Response.status(Response.Status.CREATED)
                .entity(toPayload(customer))
                .build();
    }

    @GET
    @RolesAllowed({"ADMIN", "USER"})
    public List<Map<String, Object>> listCustomers(
            @QueryParam("clinicId") Long clinicId,
            @Context HttpServletRequest httpRequest
    ) {
        TenantGuard.requireClinic(httpRequest, clinicId);
        return customerManagementService.listCustomers(clinicId).stream().map(this::toPayload).toList();
    }

    @GET
    @Path("/{customerId}")
    @RolesAllowed({"ADMIN", "USER"})
    public Map<String, Object> getCustomer(
            @PathParam("customerId") Long customerId,
            @QueryParam("clinicId") Long clinicId,
            @Context HttpServletRequest httpRequest
    ) {
        TenantGuard.requireClinic(httpRequest, clinicId);
        Customer customer = customerManagementService.findById(clinicId, customerId);
        return toPayload(customer);
    }

    @GET
    @Path("/me")
    @RolesAllowed("CUSTOMER")
    public Map<String, Object> myProfile(
            @QueryParam("clinicId") Long clinicId,
            @Context SecurityContext securityContext,
            @Context HttpServletRequest httpRequest
    ) {
        TenantGuard.requireClinic(httpRequest, clinicId);
        Customer customer = customerManagementService.findByUsername(clinicId, securityContext.getUserPrincipal().getName());
        return toPayload(customer);
    }

    @PUT
    @Path("/{customerId}")
    @RolesAllowed({"ADMIN", "USER"})
    public Map<String, Object> updateCustomer(
            @PathParam("customerId") Long customerId,
            @Valid UpdateCustomerRequest request,
            @QueryParam("clinicId") Long clinicId,
            @Context SecurityContext securityContext,
            @Context HttpServletRequest httpRequest
    ) {
        TenantGuard.requireClinic(httpRequest, clinicId);
        Customer customer = customerManagementService.updateCustomer(
                clinicId, customerId, request.fullName, request.email, request.phone
        );
        recordAudit(clinicId, securityContext, "CUSTOMER_UPDATED", customerId, null);
        return toPayload(customer);
    }

    @DELETE
    @Path("/{customerId}")
    @RolesAllowed("ADMIN")
    public Response deleteCustomer(
            @PathParam("customerId") Long customerId,
            @QueryParam("clinicId") Long clinicId,
            @Context SecurityContext securityContext,
            @Context HttpServletRequest httpRequest
    ) {
        TenantGuard.requireClinic(httpRequest, clinicId);
        customerManagementService.softDelete(clinicId, customerId);
        recordAudit(clinicId, securityContext, "CUSTOMER_DELETED", customerId, null);
        return Response.noContent().build();
    }

    private void recordAudit(Long clinicId, SecurityContext securityContext, String action, Long customerId, String details) {
        try {
            auditService.record(
                    clinicId,
                    securityContext.getUserPrincipal().getName(),
                    action,
                    "Customer",
                    customerId,
                    CorrelationIdFilter.current(),
                    details
            );
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to record audit entry for action: " + action, e);
        }
    }

    private Map<String, Object> toPayload(Customer customer) {
        return Map.of(
                "id", customer.getId(),
                "clinicId", customer.getClinicId(),
                "fullName", customer.getFullName(),
                "username", customer.getUsername(),
                "email", customer.getEmail(),
                "phone", customer.getPhone() != null ? customer.getPhone() : "",
                "deleted", customer.isDeleted(),
                "createdAt", customer.getCreatedAt().toString(),
                "updatedAt", customer.getUpdatedAt().toString()
        );
    }
}
