package com.example.clinic.api;

import com.example.clinic.api.dto.BookAppointmentRequest;
import com.example.clinic.api.dto.RescheduleRequest;
import com.example.clinic.api.dto.UpdateAppointmentStatusRequest;
import com.example.clinic.appointment.Appointment;
import com.example.clinic.appointment.AppointmentManagementService;
import com.example.clinic.appointment.AppointmentStatus;
import com.example.clinic.audit.AuditService;
import com.example.clinic.common.CorrelationIdFilter;
import com.example.clinic.schedule.DoctorSchedule;
import com.example.clinic.schedule.ScheduleManagementService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/appointments")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AppointmentResource {
    private static final Logger LOG = Logger.getLogger(AppointmentResource.class.getName());

    @Inject
    private AppointmentManagementService appointmentManagementService;

    @Inject
    private ScheduleManagementService scheduleManagementService;

    @Inject
    private AuditService auditService;

    @POST
    @RolesAllowed({"ADMIN", "USER", "CUSTOMER"})
    public Response bookAppointment(
            @Valid BookAppointmentRequest request,
            @Context SecurityContext securityContext
    ) {
        DoctorSchedule schedule = scheduleManagementService.findById(request.clinicId, request.scheduleId);
        Appointment appointment = appointmentManagementService.bookAppointment(
                request.clinicId,
                request.customerId,
                request.doctorId,
                request.scheduleId,
                schedule.getAvailableDate(),
                LocalTime.parse(request.appointmentTime),
                schedule.getStartTime(),
                schedule.getEndTime(),
                request.notes,
                securityContext.getUserPrincipal().getName()
        );
        return Response.status(Response.Status.CREATED)
                .entity(toPayload(appointment))
                .build();
    }

    @GET
    @RolesAllowed({"ADMIN", "USER"})
    public List<Map<String, Object>> listAppointments(@QueryParam("clinicId") Long clinicId) {
        return appointmentManagementService.listAppointments(clinicId).stream()
                .map(this::toPayload).toList();
    }

    @GET
    @Path("/{appointmentId}")
    @RolesAllowed({"ADMIN", "USER", "CUSTOMER"})
    public Map<String, Object> getAppointment(
            @PathParam("appointmentId") Long appointmentId,
            @QueryParam("clinicId") Long clinicId
    ) {
        Appointment appointment = appointmentManagementService.findById(clinicId, appointmentId);
        return toPayload(appointment);
    }

    @GET
    @Path("/customer/{customerId}")
    @RolesAllowed({"ADMIN", "USER", "CUSTOMER"})
    public List<Map<String, Object>> listAppointmentsByCustomer(
            @PathParam("customerId") Long customerId,
            @QueryParam("clinicId") Long clinicId
    ) {
        return appointmentManagementService.listAppointmentsByCustomer(clinicId, customerId).stream()
                .map(this::toPayload).toList();
    }

    @GET
    @Path("/doctor/{doctorId}")
    @RolesAllowed({"ADMIN", "USER", "DOCTOR"})
    public List<Map<String, Object>> listAppointmentsByDoctor(
            @PathParam("doctorId") Long doctorId,
            @QueryParam("clinicId") Long clinicId
    ) {
        return appointmentManagementService.listAppointmentsByDoctor(clinicId, doctorId).stream()
                .map(this::toPayload).toList();
    }

    @PUT
    @Path("/{appointmentId}/cancel")
    @RolesAllowed({"ADMIN", "USER", "CUSTOMER"})
    public Map<String, Object> cancelAppointment(
            @PathParam("appointmentId") Long appointmentId,
            @QueryParam("clinicId") Long clinicId
    ) {
        Appointment appointment = appointmentManagementService.cancelAppointment(clinicId, appointmentId);
        return toPayload(appointment);
    }

    @PUT
    @Path("/{appointmentId}/reschedule")
    @RolesAllowed({"ADMIN", "USER", "CUSTOMER"})
    public Map<String, Object> rescheduleAppointment(
            @PathParam("appointmentId") Long appointmentId,
            @Valid RescheduleRequest request,
            @QueryParam("clinicId") Long clinicId
    ) {
        Appointment existing = appointmentManagementService.findById(clinicId, appointmentId);
        DoctorSchedule schedule = scheduleManagementService.findById(clinicId, existing.getScheduleId());
        Appointment appointment = appointmentManagementService.reschedule(
                clinicId, appointmentId, LocalTime.parse(request.newTime),
                schedule.getStartTime(), schedule.getEndTime()
        );
        return toPayload(appointment);
    }

    @PUT
    @Path("/{appointmentId}/status")
    @RolesAllowed({"ADMIN", "USER", "DOCTOR"})
    public Map<String, Object> updateStatus(
            @PathParam("appointmentId") Long appointmentId,
            @Valid UpdateAppointmentStatusRequest request,
            @QueryParam("clinicId") Long clinicId,
            @Context SecurityContext securityContext
    ) {
        AppointmentStatus status;
        try {
            status = AppointmentStatus.valueOf(request.status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("invalid status: " + request.status);
        }
        Appointment appointment = appointmentManagementService.updateStatus(clinicId, appointmentId, status);
        recordAudit(clinicId, securityContext, "APPOINTMENT_STATUS_UPDATED", appointmentId,
                "status=" + status.name());
        return toPayload(appointment);
    }

    @DELETE
    @Path("/{appointmentId}")
    @RolesAllowed("ADMIN")
    public Response deleteAppointment(
            @PathParam("appointmentId") Long appointmentId,
            @QueryParam("clinicId") Long clinicId,
            @Context SecurityContext securityContext
    ) {
        appointmentManagementService.softDelete(clinicId, appointmentId);
        recordAudit(clinicId, securityContext, "APPOINTMENT_DELETED", appointmentId, null);
        return Response.noContent().build();
    }

    private void recordAudit(Long clinicId, SecurityContext securityContext, String action, Long appointmentId, String details) {
        try {
            auditService.record(
                    clinicId,
                    securityContext.getUserPrincipal().getName(),
                    action,
                    "Appointment",
                    appointmentId,
                    CorrelationIdFilter.current(),
                    details
            );
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to record audit entry for action: " + action, e);
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
        map.put("notes", appointment.getNotes() != null ? appointment.getNotes() : "");
        map.put("createdBy", appointment.getCreatedBy());
        map.put("deleted", appointment.isDeleted());
        map.put("rescheduleCount", appointment.getRescheduleCount());
        map.put("createdAt", appointment.getCreatedAt().toString());
        map.put("updatedAt", appointment.getUpdatedAt().toString());
        return map;
    }
}
