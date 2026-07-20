package com.example.clinic.api;

import com.example.clinic.api.dto.CreateDoctorRequest;
import com.example.clinic.api.dto.UpdateDoctorRequest;
import com.example.clinic.doctor.Doctor;
import com.example.clinic.doctor.DoctorManagementService;
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

import java.util.List;
import java.util.Map;

@Path("/doctors")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DoctorResource {
    @Inject
    private DoctorManagementService doctorManagementService;

    @POST
    @RolesAllowed({"ADMIN", "USER"})
    public Response createDoctor(@Valid CreateDoctorRequest request) {
        Doctor doctor = doctorManagementService.createDoctor(
                request.clinicId,
                request.fullName,
                request.username,
                request.specialty,
                request.active,
                request.slotMinutes
        );
        return Response.status(Response.Status.CREATED)
                .entity(toPayload(doctor))
                .build();
    }

    @GET
    @RolesAllowed({"ADMIN", "USER", "CUSTOMER"})
    public List<Map<String, Object>> listDoctors(@QueryParam("clinicId") Long clinicId) {
        return doctorManagementService.listDoctors(clinicId).stream().map(this::toPayload).toList();
    }

    @GET
    @Path("/{doctorId}")
    @RolesAllowed({"ADMIN", "USER", "CUSTOMER"})
    public Map<String, Object> getDoctor(
            @PathParam("doctorId") Long doctorId,
            @QueryParam("clinicId") Long clinicId
    ) {
        Doctor doctor = doctorManagementService.findById(clinicId, doctorId);
        return toPayload(doctor);
    }

    @PUT
    @Path("/{doctorId}")
    @RolesAllowed({"ADMIN", "USER"})
    public Map<String, Object> updateDoctor(
            @PathParam("doctorId") Long doctorId,
            @Valid UpdateDoctorRequest request,
            @QueryParam("clinicId") Long clinicId
    ) {
        Doctor doctor = doctorManagementService.updateDoctor(
                clinicId, doctorId, request.fullName, request.specialty, request.active, request.slotMinutes
        );
        return toPayload(doctor);
    }

    @DELETE
    @Path("/{doctorId}")
    @RolesAllowed("ADMIN")
    public Response deleteDoctor(
            @PathParam("doctorId") Long doctorId,
            @QueryParam("clinicId") Long clinicId
    ) {
        doctorManagementService.softDelete(clinicId, doctorId);
        return Response.noContent().build();
    }

    private Map<String, Object> toPayload(Doctor doctor) {
        return Map.of(
                "id", doctor.getId(),
                "clinicId", doctor.getClinicId(),
                "fullName", doctor.getFullName(),
                "username", doctor.getUsername(),
                "specialty", doctor.getSpecialty(),
                "active", doctor.isActive(),
                "deleted", doctor.isDeleted(),
                "slotMinutes", doctor.getSlotMinutes(),
                "createdAt", doctor.getCreatedAt().toString(),
                "updatedAt", doctor.getUpdatedAt().toString()
        );
    }
}
