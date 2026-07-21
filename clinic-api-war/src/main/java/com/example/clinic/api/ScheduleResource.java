package com.example.clinic.api;

import com.example.clinic.api.dto.CreateScheduleRequest;
import com.example.clinic.api.dto.UpdateScheduleRequest;
import com.example.clinic.audit.AuditService;
import com.example.clinic.common.CorrelationIdFilter;
import com.example.clinic.schedule.DoctorSchedule;
import com.example.clinic.schedule.ScheduleManagementService;
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

@Path("/doctors/{doctorId}/schedules")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ScheduleResource {
    private static final Logger LOG = Logger.getLogger(ScheduleResource.class.getName());

    @Inject
    private ScheduleManagementService scheduleManagementService;

    @Inject
    private AuditService auditService;

    @POST
    @RolesAllowed({"ADMIN", "USER"})
    public Response createSchedule(
            @PathParam("doctorId") Long doctorId,
            @Valid CreateScheduleRequest request,
            @Context SecurityContext securityContext,
            @Context HttpServletRequest httpRequest
    ) {
        TenantGuard.requireClinic(httpRequest, request.clinicId);
        DoctorSchedule schedule = scheduleManagementService.addSchedule(
                request.clinicId,
                doctorId,
                DateTimeParams.parseDate(request.availableDate, "availableDate"),
                DateTimeParams.parseTime(request.startTime, "startTime"),
                DateTimeParams.parseTime(request.endTime, "endTime"),
                request.capacity
        );
        recordAudit(request.clinicId, securityContext, "SCHEDULE_CREATED", schedule.getId(),
                "doctorId=" + doctorId);
        return Response.status(Response.Status.CREATED)
                .entity(toPayload(schedule))
                .build();
    }

    @GET
    @RolesAllowed({"ADMIN", "USER", "CUSTOMER"})
    public List<Map<String, Object>> listSchedules(
            @PathParam("doctorId") Long doctorId,
            @QueryParam("clinicId") Long clinicId,
            @Context HttpServletRequest httpRequest
    ) {
        TenantGuard.requireClinic(httpRequest, clinicId);
        return scheduleManagementService.listSchedules(clinicId, doctorId).stream()
                .map(this::toPayload).toList();
    }

    @GET
    @Path("/{scheduleId}")
    @RolesAllowed({"ADMIN", "USER", "CUSTOMER"})
    public Map<String, Object> getSchedule(
            @PathParam("doctorId") Long doctorId,
            @PathParam("scheduleId") Long scheduleId,
            @QueryParam("clinicId") Long clinicId,
            @Context HttpServletRequest httpRequest
    ) {
        TenantGuard.requireClinic(httpRequest, clinicId);
        DoctorSchedule schedule = scheduleManagementService.findById(clinicId, scheduleId);
        return toPayload(schedule);
    }

    @GET
    @Path("/by-date")
    @RolesAllowed({"ADMIN", "USER", "CUSTOMER"})
    public List<Map<String, Object>> listSchedulesByDate(
            @PathParam("doctorId") Long doctorId,
            @QueryParam("clinicId") Long clinicId,
            @QueryParam("date") String date,
            @Context HttpServletRequest httpRequest
    ) {
        TenantGuard.requireClinic(httpRequest, clinicId);
        return scheduleManagementService.listSchedulesByDate(clinicId, doctorId, DateTimeParams.parseDate(date, "date")).stream()
                .map(this::toPayload).toList();
    }

    @PUT
    @Path("/{scheduleId}")
    @RolesAllowed({"ADMIN", "USER"})
    public Map<String, Object> updateSchedule(
            @PathParam("doctorId") Long doctorId,
            @PathParam("scheduleId") Long scheduleId,
            @Valid UpdateScheduleRequest request,
            @QueryParam("clinicId") Long clinicId,
            @Context SecurityContext securityContext,
            @Context HttpServletRequest httpRequest
    ) {
        TenantGuard.requireClinic(httpRequest, clinicId);
        DoctorSchedule schedule = scheduleManagementService.updateSchedule(
                clinicId,
                scheduleId,
                request.startTime != null ? DateTimeParams.parseTime(request.startTime, "startTime") : null,
                request.endTime != null ? DateTimeParams.parseTime(request.endTime, "endTime") : null,
                request.capacity
        );
        recordAudit(clinicId, securityContext, "SCHEDULE_UPDATED", scheduleId, null);
        return toPayload(schedule);
    }

    @DELETE
    @Path("/{scheduleId}")
    @RolesAllowed("ADMIN")
    public Response deleteSchedule(
            @PathParam("doctorId") Long doctorId,
            @PathParam("scheduleId") Long scheduleId,
            @QueryParam("clinicId") Long clinicId,
            @Context SecurityContext securityContext,
            @Context HttpServletRequest httpRequest
    ) {
        TenantGuard.requireClinic(httpRequest, clinicId);
        scheduleManagementService.softDelete(clinicId, scheduleId);
        recordAudit(clinicId, securityContext, "SCHEDULE_DELETED", scheduleId, null);
        return Response.noContent().build();
    }

    private void recordAudit(Long clinicId, SecurityContext securityContext, String action, Long scheduleId, String details) {
        try {
            auditService.record(
                    clinicId,
                    securityContext.getUserPrincipal().getName(),
                    action,
                    "DoctorSchedule",
                    scheduleId,
                    CorrelationIdFilter.current(),
                    details
            );
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to record audit entry for action: " + action, e);
        }
    }

    private Map<String, Object> toPayload(DoctorSchedule schedule) {
        return Map.of(
                "id", schedule.getId(),
                "clinicId", schedule.getClinicId(),
                "doctorId", schedule.getDoctorId(),
                "availableDate", schedule.getAvailableDate().toString(),
                "startTime", schedule.getStartTime().toString(),
                "endTime", schedule.getEndTime().toString(),
                "capacity", schedule.getCapacity(),
                "deleted", schedule.isDeleted(),
                "createdAt", schedule.getCreatedAt().toString(),
                "updatedAt", schedule.getUpdatedAt().toString()
        );
    }
}
