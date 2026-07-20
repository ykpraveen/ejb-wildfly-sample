package com.example.clinic.api;

import com.example.clinic.api.dto.CreateScheduleRequest;
import com.example.clinic.api.dto.UpdateScheduleRequest;
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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Path("/doctors/{doctorId}/schedules")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ScheduleResource {
    @Inject
    private ScheduleManagementService scheduleManagementService;

    @POST
    @RolesAllowed({"ADMIN", "USER"})
    public Response createSchedule(
            @PathParam("doctorId") Long doctorId,
            @Valid CreateScheduleRequest request
    ) {
        DoctorSchedule schedule = scheduleManagementService.addSchedule(
                request.clinicId,
                doctorId,
                LocalDate.parse(request.availableDate),
                LocalTime.parse(request.startTime),
                LocalTime.parse(request.endTime),
                request.capacity
        );
        return Response.status(Response.Status.CREATED)
                .entity(toPayload(schedule))
                .build();
    }

    @GET
    @RolesAllowed({"ADMIN", "USER", "CUSTOMER"})
    public List<Map<String, Object>> listSchedules(
            @PathParam("doctorId") Long doctorId,
            @QueryParam("clinicId") Long clinicId
    ) {
        return scheduleManagementService.listSchedules(clinicId, doctorId).stream()
                .map(this::toPayload).toList();
    }

    @GET
    @Path("/{scheduleId}")
    @RolesAllowed({"ADMIN", "USER", "CUSTOMER"})
    public Map<String, Object> getSchedule(
            @PathParam("doctorId") Long doctorId,
            @PathParam("scheduleId") Long scheduleId,
            @QueryParam("clinicId") Long clinicId
    ) {
        DoctorSchedule schedule = scheduleManagementService.findById(clinicId, scheduleId);
        return toPayload(schedule);
    }

    @GET
    @Path("/by-date")
    @RolesAllowed({"ADMIN", "USER", "CUSTOMER"})
    public List<Map<String, Object>> listSchedulesByDate(
            @PathParam("doctorId") Long doctorId,
            @QueryParam("clinicId") Long clinicId,
            @QueryParam("date") String date
    ) {
        return scheduleManagementService.listSchedulesByDate(clinicId, doctorId, LocalDate.parse(date)).stream()
                .map(this::toPayload).toList();
    }

    @PUT
    @Path("/{scheduleId}")
    @RolesAllowed({"ADMIN", "USER"})
    public Map<String, Object> updateSchedule(
            @PathParam("doctorId") Long doctorId,
            @PathParam("scheduleId") Long scheduleId,
            @Valid UpdateScheduleRequest request,
            @QueryParam("clinicId") Long clinicId
    ) {
        DoctorSchedule schedule = scheduleManagementService.updateSchedule(
                clinicId,
                scheduleId,
                request.startTime != null ? LocalTime.parse(request.startTime) : null,
                request.endTime != null ? LocalTime.parse(request.endTime) : null,
                request.capacity
        );
        return toPayload(schedule);
    }

    @DELETE
    @Path("/{scheduleId}")
    @RolesAllowed("ADMIN")
    public Response deleteSchedule(
            @PathParam("doctorId") Long doctorId,
            @PathParam("scheduleId") Long scheduleId,
            @QueryParam("clinicId") Long clinicId
    ) {
        scheduleManagementService.softDelete(clinicId, scheduleId);
        return Response.noContent().build();
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
