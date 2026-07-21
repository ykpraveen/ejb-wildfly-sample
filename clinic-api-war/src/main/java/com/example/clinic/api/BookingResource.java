package com.example.clinic.api;

import com.example.clinic.api.dto.AddNotesRequest;
import com.example.clinic.api.dto.SelectDoctorRequest;
import com.example.clinic.api.dto.SelectScheduleRequest;
import com.example.clinic.api.dto.SelectTimeRequest;
import com.example.clinic.api.dto.StartBookingRequest;
import com.example.clinic.appointment.Appointment;
import com.example.clinic.appointment.BookingSessionBean;
import com.example.clinic.appointment.BookingSessionRegistry;
import com.example.clinic.appointment.BookingSummary;
import com.example.clinic.customer.Customer;
import com.example.clinic.customer.CustomerManagementService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.util.LinkedHashMap;
import java.util.Map;

@Path("/bookings")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class BookingResource {

    @EJB
    private BookingSessionRegistry sessionRegistry;

    @Inject
    private CustomerManagementService customerManagementService;

    @POST
    @RolesAllowed({"ADMIN", "USER", "CUSTOMER"})
    public Response startBooking(
            @Valid StartBookingRequest request,
            @Context SecurityContext securityContext,
            @Context ContainerRequestContext requestContext
    ) {
        TenantGuard.requireClinic(requestContext, request.clinicId);
        enforceCustomerOwnership(securityContext, request.clinicId, request.customerId);
        String sessionId = sessionRegistry.createSession();
        BookingSessionBean session = sessionRegistry.getSession(sessionId);
        session.startBooking(request.clinicId, request.customerId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", sessionId);
        return Response.ok(result).build();
    }

    @GET
    @Path("/{sessionId}")
    @RolesAllowed({"ADMIN", "USER", "CUSTOMER"})
    public BookingSummary getSummary(
            @PathParam("sessionId") String sessionId,
            @Context SecurityContext securityContext,
            @Context ContainerRequestContext requestContext
    ) {
        BookingSessionBean session = sessionRegistry.getSession(sessionId);
        return assertSessionAccess(session, sessionId, securityContext, requestContext);
    }

    @PUT
    @Path("/{sessionId}/doctor")
    @RolesAllowed({"ADMIN", "USER", "CUSTOMER"})
    public Map<String, String> selectDoctor(@PathParam("sessionId") String sessionId,
                                            @Valid SelectDoctorRequest request,
                                            @Context SecurityContext securityContext,
                                            @Context ContainerRequestContext requestContext) {
        BookingSessionBean session = sessionRegistry.getSession(sessionId);
        assertSessionAccess(session, sessionId, securityContext, requestContext);
        session.selectDoctor(request.doctorId);
        return Map.of("status", "OK");
    }

    @PUT
    @Path("/{sessionId}/schedule")
    @RolesAllowed({"ADMIN", "USER", "CUSTOMER"})
    public Map<String, String> selectSchedule(@PathParam("sessionId") String sessionId,
                                              @Valid SelectScheduleRequest request,
                                              @Context SecurityContext securityContext,
                                              @Context ContainerRequestContext requestContext) {
        BookingSessionBean session = sessionRegistry.getSession(sessionId);
        assertSessionAccess(session, sessionId, securityContext, requestContext);
        session.selectSchedule(request.scheduleId);
        return Map.of("status", "OK");
    }

    @PUT
    @Path("/{sessionId}/time")
    @RolesAllowed({"ADMIN", "USER", "CUSTOMER"})
    public Map<String, String> selectTime(@PathParam("sessionId") String sessionId,
                                          @Valid SelectTimeRequest request,
                                          @Context SecurityContext securityContext,
                                          @Context ContainerRequestContext requestContext) {
        BookingSessionBean session = sessionRegistry.getSession(sessionId);
        assertSessionAccess(session, sessionId, securityContext, requestContext);
        session.selectTime(DateTimeParams.parseTime(request.appointmentTime, "appointmentTime"));
        return Map.of("status", "OK");
    }

    @PUT
    @Path("/{sessionId}/notes")
    @RolesAllowed({"ADMIN", "USER", "CUSTOMER"})
    public Map<String, String> addNotes(@PathParam("sessionId") String sessionId,
                                        @Valid AddNotesRequest request,
                                        @Context SecurityContext securityContext,
                                        @Context ContainerRequestContext requestContext) {
        BookingSessionBean session = sessionRegistry.getSession(sessionId);
        assertSessionAccess(session, sessionId, securityContext, requestContext);
        session.addNotes(request.notes);
        return Map.of("status", "OK");
    }

    @POST
    @Path("/{sessionId}/confirm")
    @RolesAllowed({"ADMIN", "USER", "CUSTOMER"})
    public Response confirmBooking(@PathParam("sessionId") String sessionId,
                                   @Context SecurityContext securityContext,
                                   @Context ContainerRequestContext requestContext) {
        BookingSessionBean session = sessionRegistry.getSession(sessionId);
        assertSessionAccess(session, sessionId, securityContext, requestContext);
        String actor = securityContext.getUserPrincipal().getName();
        Appointment appointment = session.confirmBooking(actor);
        sessionRegistry.removeSession(sessionId);
        return Response.ok(toPayload(appointment)).build();
    }

    @DELETE
    @Path("/{sessionId}")
    @RolesAllowed({"ADMIN", "USER", "CUSTOMER"})
    public Map<String, String> cancelBooking(@PathParam("sessionId") String sessionId,
                                             @Context SecurityContext securityContext,
                                             @Context ContainerRequestContext requestContext) {
        BookingSessionBean session = sessionRegistry.getSession(sessionId);
        assertSessionAccess(session, sessionId, securityContext, requestContext);
        session.cancel();
        sessionRegistry.removeSession(sessionId);
        return Map.of("status", "CANCELLED");
    }

    /**
     * A booking session is created for one clinic/customer; every subsequent call on that
     * sessionId must be made by the same tenant (and, for CUSTOMER-only callers, the same
     * customer) — otherwise anyone holding a foreign sessionId could drive someone else's
     * in-progress booking.
     */
    private BookingSummary assertSessionAccess(BookingSessionBean session, String sessionId,
                                                SecurityContext securityContext, ContainerRequestContext requestContext) {
        BookingSummary summary = session.getSummary(sessionId);
        TenantGuard.requireClinic(requestContext, summary.clinicId());
        enforceCustomerOwnership(securityContext, summary.clinicId(), summary.customerId());
        return summary;
    }

    private void enforceCustomerOwnership(SecurityContext securityContext, Long clinicId, Long customerId) {
        if (securityContext.isUserInRole("ADMIN") || securityContext.isUserInRole("USER")) {
            return;
        }
        Customer caller = customerManagementService.findByUsername(clinicId, securityContext.getUserPrincipal().getName());
        if (!caller.getId().equals(customerId)) {
            throw new ForbiddenException("customers may only access their own booking sessions");
        }
    }

    private Map<String, Object> toPayload(Appointment appointment) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", appointment.getId());
        map.put("clinicId", appointment.getClinicId());
        map.put("customerId", appointment.getCustomerId());
        map.put("doctorId", appointment.getDoctorId());
        map.put("scheduleId", appointment.getScheduleId());
        map.put("appointmentDate", appointment.getAppointmentDate().toString());
        map.put("appointmentTime", appointment.getAppointmentTime().toString());
        map.put("status", appointment.getStatus().name());
        map.put("notes", appointment.getNotes());
        map.put("createdBy", appointment.getCreatedBy());
        map.put("deleted", appointment.isDeleted());
        map.put("rescheduleCount", appointment.getRescheduleCount());
        map.put("createdAt", appointment.getCreatedAt().toString());
        map.put("updatedAt", appointment.getUpdatedAt().toString());
        return map;
    }
}
