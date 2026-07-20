package com.example.clinic.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Path("/openapi")
@Produces(MediaType.APPLICATION_JSON)
public class OpenApiResource {

    @GET
    public Map<String, Object> openApi() {
        Map<String, Object> spec = new LinkedHashMap<>();
        spec.put("openapi", "3.0.3");
        spec.put("info", Map.of(
                "title", "Clinic Backend API",
                "version", "1.0.0",
                "description", "Clinic appointment management backend API"
        ));
        spec.put("servers", List.of(Map.of("url", "/clinic-api/api", "description", "WildFly local")));
        spec.put("paths", buildPaths());
        spec.put("components", buildComponents());
        spec.put("security", List.of(Map.of("bearerAuth", List.of())));
        return spec;
    }

    private Map<String, Object> buildPaths() {
        Map<String, Object> paths = new LinkedHashMap<>();

        paths.put("/auth/login", Map.of("post", Map.of(
                "summary", "Authenticate user and return JWT",
                "tags", List.of("Auth"),
                "requestBody", Map.of("required", true, "content", Map.of(
                        "application/json", Map.of("schema", Map.of("$ref", "#/components/schemas/LoginRequest"))
                )),
                "responses", Map.of("200", Map.of("description", "JWT token returned"),
                        "401", Map.of("description", "Invalid credentials"))
        )));

        paths.put("/users", Map.of(
                "get", Map.of(
                        "summary", "List users for a clinic",
                        "tags", List.of("User Management"),
                        "security", List.of(Map.of("bearerAuth", List.of())),
                        "parameters", List.of(param("clinicId", "query", "integer")),
                        "responses", Map.of("200", Map.of("description", "List of users"))
                ),
                "post", Map.of(
                        "summary", "Create a new user (ADMIN only)",
                        "tags", List.of("User Management"),
                        "security", List.of(Map.of("bearerAuth", List.of())),
                        "requestBody", jsonBody("#/components/schemas/CreateUserRequest"),
                        "responses", Map.of("201", Map.of("description", "User created"),
                                "403", Map.of("description", "ADMIN role required"))
                )
        ));

        paths.put("/users/{userId}/activate", Map.of("patch", Map.of(
                "summary", "Activate a user (ADMIN only)",
                "tags", List.of("User Management"),
                "security", List.of(Map.of("bearerAuth", List.of())),
                "parameters", List.of(pathParam("userId", "integer")),
                "requestBody", jsonBody("#/components/schemas/ActivateUserRequest"),
                "responses", Map.of("200", Map.of("description", "User activated"))
        )));

        paths.put("/users/{userId}/deactivate", Map.of("patch", Map.of(
                "summary", "Deactivate a user (ADMIN only)",
                "tags", List.of("User Management"),
                "security", List.of(Map.of("bearerAuth", List.of())),
                "parameters", List.of(pathParam("userId", "integer")),
                "requestBody", jsonBody("#/components/schemas/ActivateUserRequest"),
                "responses", Map.of("200", Map.of("description", "User deactivated"))
        )));

        paths.put("/customers", Map.of(
                "get", Map.of(
                        "summary", "List customers for a clinic",
                        "tags", List.of("Customer Management"),
                        "security", List.of(Map.of("bearerAuth", List.of())),
                        "parameters", List.of(param("clinicId", "query", "integer")),
                        "responses", Map.of("200", Map.of("description", "List of customers"))
                ),
                "post", Map.of(
                        "summary", "Create a new customer",
                        "tags", List.of("Customer Management"),
                        "security", List.of(Map.of("bearerAuth", List.of())),
                        "requestBody", jsonBody("#/components/schemas/CreateCustomerRequest"),
                        "responses", Map.of("201", Map.of("description", "Customer created"))
                )
        ));

        Map<String, Object> customerById = new LinkedHashMap<>();
        customerById.put("get", Map.of(
                "summary", "Get customer by ID",
                "tags", List.of("Customer Management"),
                "security", List.of(Map.of("bearerAuth", List.of())),
                "parameters", List.of(pathParam("customerId", "integer")),
                "responses", Map.of("200", Map.of("description", "Customer details"))
        ));
        customerById.put("put", Map.of(
                "summary", "Update customer details",
                "tags", List.of("Customer Management"),
                "security", List.of(Map.of("bearerAuth", List.of())),
                "parameters", List.of(pathParam("customerId", "integer")),
                "responses", Map.of("200", Map.of("description", "Customer updated"))
        ));
        customerById.put("delete", Map.of(
                "summary", "Soft-delete a customer (ADMIN only)",
                "tags", List.of("Customer Management"),
                "security", List.of(Map.of("bearerAuth", List.of())),
                "parameters", List.of(pathParam("customerId", "integer")),
                "responses", Map.of("204", Map.of("description", "Customer deleted"))
        ));
        paths.put("/customers/{customerId}", customerById);

        paths.put("/doctors", Map.of(
                "get", Map.of(
                        "summary", "List doctors for a clinic",
                        "tags", List.of("Doctor Management"),
                        "security", List.of(Map.of("bearerAuth", List.of())),
                        "parameters", List.of(param("clinicId", "query", "integer")),
                        "responses", Map.of("200", Map.of("description", "List of doctors"))
                ),
                "post", Map.of(
                        "summary", "Create a new doctor",
                        "tags", List.of("Doctor Management"),
                        "security", List.of(Map.of("bearerAuth", List.of())),
                        "requestBody", jsonBody("#/components/schemas/CreateDoctorRequest"),
                        "responses", Map.of("201", Map.of("description", "Doctor created"))
                )
        ));

        paths.put("/doctors/{doctorId}", Map.of(
                "get", Map.of(
                        "summary", "Get doctor by ID",
                        "tags", List.of("Doctor Management"),
                        "security", List.of(Map.of("bearerAuth", List.of())),
                        "parameters", List.of(pathParam("doctorId", "integer")),
                        "responses", Map.of("200", Map.of("description", "Doctor details"))
                ),
                "put", Map.of(
                        "summary", "Update doctor details",
                        "tags", List.of("Doctor Management"),
                        "security", List.of(Map.of("bearerAuth", List.of())),
                        "parameters", List.of(pathParam("doctorId", "integer")),
                        "responses", Map.of("200", Map.of("description", "Doctor updated"))
                ),
                "delete", Map.of(
                        "summary", "Soft-delete a doctor (ADMIN only)",
                        "tags", List.of("Doctor Management"),
                        "security", List.of(Map.of("bearerAuth", List.of())),
                        "parameters", List.of(pathParam("doctorId", "integer")),
                        "responses", Map.of("204", Map.of("description", "Doctor deleted"))
                )
        ));

        paths.put("/doctors/{doctorId}/schedules", Map.of(
                "get", Map.of(
                        "summary", "List schedules for a doctor",
                        "tags", List.of("Schedule Management"),
                        "security", List.of(Map.of("bearerAuth", List.of())),
                        "parameters", List.of(pathParam("doctorId", "integer")),
                        "responses", Map.of("200", Map.of("description", "List of schedules"))
                ),
                "post", Map.of(
                        "summary", "Add a schedule for a doctor",
                        "tags", List.of("Schedule Management"),
                        "security", List.of(Map.of("bearerAuth", List.of())),
                        "parameters", List.of(pathParam("doctorId", "integer")),
                        "requestBody", jsonBody("#/components/schemas/CreateScheduleRequest"),
                        "responses", Map.of("201", Map.of("description", "Schedule created"))
                )
        ));

        paths.put("/doctors/{doctorId}/schedules/{scheduleId}", Map.of(
                "get", Map.of(
                        "summary", "Get schedule by ID",
                        "tags", List.of("Schedule Management"),
                        "security", List.of(Map.of("bearerAuth", List.of())),
                        "parameters", List.of(pathParam("doctorId", "integer"), pathParam("scheduleId", "integer")),
                        "responses", Map.of("200", Map.of("description", "Schedule details"))
                ),
                "delete", Map.of(
                        "summary", "Delete a schedule (ADMIN only)",
                        "tags", List.of("Schedule Management"),
                        "security", List.of(Map.of("bearerAuth", List.of())),
                        "parameters", List.of(pathParam("doctorId", "integer"), pathParam("scheduleId", "integer")),
                        "responses", Map.of("204", Map.of("description", "Schedule deleted"))
                )
        ));

        paths.put("/appointments", Map.of(
                "get", Map.of(
                        "summary", "List appointments for a clinic",
                        "tags", List.of("Appointment Management"),
                        "security", List.of(Map.of("bearerAuth", List.of())),
                        "parameters", List.of(param("clinicId", "query", "integer")),
                        "responses", Map.of("200", Map.of("description", "List of appointments"))
                ),
                "post", Map.of(
                        "summary", "Book a new appointment",
                        "tags", List.of("Appointment Management"),
                        "security", List.of(Map.of("bearerAuth", List.of())),
                        "requestBody", jsonBody("#/components/schemas/BookAppointmentRequest"),
                        "responses", Map.of("201", Map.of("description", "Appointment booked"),
                                "400", Map.of("description", "Booking rule violation"))
                )
        ));

        paths.put("/appointments/{appointmentId}", Map.of(
                "get", Map.of(
                        "summary", "Get appointment by ID",
                        "tags", List.of("Appointment Management"),
                        "security", List.of(Map.of("bearerAuth", List.of())),
                        "parameters", List.of(pathParam("appointmentId", "integer")),
                        "responses", Map.of("200", Map.of("description", "Appointment details"))
                )
        ));

        Map<String, Object> cancel = new LinkedHashMap<>();
        cancel.put("put", Map.of(
                "summary", "Cancel an appointment",
                "tags", List.of("Appointment Management"),
                "security", List.of(Map.of("bearerAuth", List.of())),
                "parameters", List.of(pathParam("appointmentId", "integer")),
                "responses", Map.of("200", Map.of("description", "Appointment cancelled"))
        ));
        paths.put("/appointments/{appointmentId}/cancel", cancel);

        Map<String, Object> reschedule = new LinkedHashMap<>();
        reschedule.put("put", Map.of(
                "summary", "Reschedule an appointment",
                "tags", List.of("Appointment Management"),
                "security", List.of(Map.of("bearerAuth", List.of())),
                "parameters", List.of(pathParam("appointmentId", "integer")),
                "responses", Map.of("200", Map.of("description", "Appointment rescheduled"))
        ));
        paths.put("/appointments/{appointmentId}/reschedule", reschedule);

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("put", Map.of(
                "summary", "Update appointment status",
                "tags", List.of("Appointment Management"),
                "security", List.of(Map.of("bearerAuth", List.of())),
                "parameters", List.of(pathParam("appointmentId", "integer")),
                "responses", Map.of("200", Map.of("description", "Status updated"))
        ));
        paths.put("/appointments/{appointmentId}/status", status);

        paths.put("/health", Map.of("get", Map.of(
                "summary", "Health check endpoint",
                "tags", List.of("Operations"),
                "responses", Map.of("200", Map.of("description", "Service is UP"))
        )));

        paths.put("/ready", Map.of("get", Map.of(
                "summary", "Readiness check endpoint",
                "tags", List.of("Operations"),
                "responses", Map.of("200", Map.of("description", "Service is ready"))
        )));

        paths.put("/bookings", Map.of(
                "post", Map.of(
                        "summary", "Start a booking session",
                        "tags", List.of("Booking Wizard"),
                        "security", List.of(Map.of("bearerAuth", List.of())),
                        "requestBody", jsonBody("#/components/schemas/StartBookingRequest"),
                        "responses", Map.of("201", Map.of("description", "Booking session created"))
                )
        ));

        paths.put("/bookings/{sessionId}", Map.of(
                "get", Map.of(
                        "summary", "Get booking summary",
                        "tags", List.of("Booking Wizard"),
                        "security", List.of(Map.of("bearerAuth", List.of())),
                        "parameters", List.of(stringPathParam("sessionId")),
                        "responses", Map.of("200", Map.of("description", "Booking summary"))
                ),
                "delete", Map.of(
                        "summary", "Cancel booking session",
                        "tags", List.of("Booking Wizard"),
                        "security", List.of(Map.of("bearerAuth", List.of())),
                        "parameters", List.of(stringPathParam("sessionId")),
                        "responses", Map.of("204", Map.of("description", "Booking session cancelled"))
                )
        ));

        paths.put("/bookings/{sessionId}/doctor", Map.of("put", Map.of(
                "summary", "Select doctor",
                "tags", List.of("Booking Wizard"),
                "security", List.of(Map.of("bearerAuth", List.of())),
                "parameters", List.of(stringPathParam("sessionId")),
                "requestBody", jsonBody("#/components/schemas/SelectDoctorRequest"),
                "responses", Map.of("200", Map.of("description", "Doctor selected"))
        )));

        paths.put("/bookings/{sessionId}/schedule", Map.of("put", Map.of(
                "summary", "Select schedule",
                "tags", List.of("Booking Wizard"),
                "security", List.of(Map.of("bearerAuth", List.of())),
                "parameters", List.of(stringPathParam("sessionId")),
                "requestBody", jsonBody("#/components/schemas/SelectScheduleRequest"),
                "responses", Map.of("200", Map.of("description", "Schedule selected"))
        )));

        paths.put("/bookings/{sessionId}/time", Map.of("put", Map.of(
                "summary", "Select time",
                "tags", List.of("Booking Wizard"),
                "security", List.of(Map.of("bearerAuth", List.of())),
                "parameters", List.of(stringPathParam("sessionId")),
                "requestBody", jsonBody("#/components/schemas/SelectTimeRequest"),
                "responses", Map.of("200", Map.of("description", "Time selected"))
        )));

        paths.put("/bookings/{sessionId}/notes", Map.of("put", Map.of(
                "summary", "Add notes",
                "tags", List.of("Booking Wizard"),
                "security", List.of(Map.of("bearerAuth", List.of())),
                "parameters", List.of(stringPathParam("sessionId")),
                "requestBody", jsonBody("#/components/schemas/AddNotesRequest"),
                "responses", Map.of("200", Map.of("description", "Notes added"))
        )));

        paths.put("/bookings/{sessionId}/confirm", Map.of("post", Map.of(
                "summary", "Confirm booking",
                "tags", List.of("Booking Wizard"),
                "security", List.of(Map.of("bearerAuth", List.of())),
                "parameters", List.of(stringPathParam("sessionId")),
                "responses", Map.of("200", Map.of("description", "Appointment created"),
                        "400", Map.of("description", "Booking rule violation"))
        )));

        return paths;
    }

    private Map<String, Object> buildComponents() {
        Map<String, Object> components = new LinkedHashMap<>();

        Map<String, Object> schemas = new LinkedHashMap<>();

        schemas.put("LoginRequest", Map.of(
                "type", "object",
                "properties", Map.of(
                        "clinicId", Map.of("type", "integer", "format", "int64"),
                        "username", Map.of("type", "string"),
                        "password", Map.of("type", "string")
                )
        ));

        schemas.put("CreateUserRequest", Map.of(
                "type", "object",
                "properties", Map.of(
                        "clinicId", Map.of("type", "integer", "format", "int64"),
                        "username", Map.of("type", "string"),
                        "password", Map.of("type", "string"),
                        "roles", Map.of("type", "array", "items", Map.of("type", "string"))
                )
        ));

        schemas.put("ActivateUserRequest", Map.of(
                "type", "object",
                "properties", Map.of(
                        "clinicId", Map.of("type", "integer", "format", "int64")
                )
        ));

        schemas.put("CreateCustomerRequest", Map.of(
                "type", "object",
                "properties", Map.of(
                        "clinicId", Map.of("type", "integer", "format", "int64"),
                        "fullName", Map.of("type", "string"),
                        "username", Map.of("type", "string"),
                        "email", Map.of("type", "string", "format", "email"),
                        "phone", Map.of("type", "string")
                )
        ));

        schemas.put("CreateDoctorRequest", Map.of(
                "type", "object",
                "properties", Map.of(
                        "clinicId", Map.of("type", "integer", "format", "int64"),
                        "fullName", Map.of("type", "string"),
                        "username", Map.of("type", "string"),
                        "specialty", Map.of("type", "string"),
                        "active", Map.of("type", "boolean"),
                        "slotMinutes", Map.of("type", "integer")
                )
        ));

        schemas.put("CreateScheduleRequest", Map.of(
                "type", "object",
                "properties", Map.of(
                        "clinicId", Map.of("type", "integer", "format", "int64"),
                        "doctorId", Map.of("type", "integer", "format", "int64"),
                        "availableDate", Map.of("type", "string", "format", "date"),
                        "startTime", Map.of("type", "string", "format", "time"),
                        "endTime", Map.of("type", "string", "format", "time"),
                        "capacity", Map.of("type", "integer")
                )
        ));

        schemas.put("BookAppointmentRequest", Map.of(
                "type", "object",
                "properties", Map.of(
                        "clinicId", Map.of("type", "integer", "format", "int64"),
                        "customerId", Map.of("type", "integer", "format", "int64"),
                        "doctorId", Map.of("type", "integer", "format", "int64"),
                        "scheduleId", Map.of("type", "integer", "format", "int64"),
                        "appointmentTime", Map.of("type", "string", "format", "time"),
                        "notes", Map.of("type", "string")
                )
        ));

        schemas.put("StartBookingRequest", Map.of(
                "type", "object",
                "properties", Map.of(
                        "clinicId", Map.of("type", "integer", "format", "int64"),
                        "customerId", Map.of("type", "integer", "format", "int64")
                )
        ));

        schemas.put("SelectDoctorRequest", Map.of(
                "type", "object",
                "properties", Map.of(
                        "doctorId", Map.of("type", "integer", "format", "int64")
                )
        ));

        schemas.put("SelectScheduleRequest", Map.of(
                "type", "object",
                "properties", Map.of(
                        "scheduleId", Map.of("type", "integer", "format", "int64")
                )
        ));

        schemas.put("SelectTimeRequest", Map.of(
                "type", "object",
                "properties", Map.of(
                        "appointmentTime", Map.of("type", "string")
                )
        ));

        schemas.put("AddNotesRequest", Map.of(
                "type", "object",
                "properties", Map.of(
                        "notes", Map.of("type", "string")
                )
        ));

        schemas.put("BookingSummary", Map.of(
                "type", "object",
                "properties", Map.ofEntries(
                        Map.entry("sessionId", Map.of("type", "string")),
                        Map.entry("clinicId", Map.of("type", "integer")),
                        Map.entry("customerId", Map.of("type", "integer")),
                        Map.entry("doctorId", Map.of("type", "integer")),
                        Map.entry("doctorName", Map.of("type", "string")),
                        Map.entry("scheduleId", Map.of("type", "integer")),
                        Map.entry("scheduleDate", Map.of("type", "string", "format", "date")),
                        Map.entry("scheduleStartTime", Map.of("type", "string")),
                        Map.entry("scheduleEndTime", Map.of("type", "string")),
                        Map.entry("selectedTime", Map.of("type", "string")),
                        Map.entry("notes", Map.of("type", "string")),
                        Map.entry("status", Map.of("type", "string"))
                )
        ));

        schemas.put("RescheduleRequest", Map.of(
                "type", "object",
                "properties", Map.of(
                        "newTime", Map.of("type", "string")
                )
        ));

        schemas.put("UpdateAppointmentStatusRequest", Map.of(
                "type", "object",
                "properties", Map.of(
                        "status", Map.of("type", "string")
                )
        ));

        schemas.put("UpdateCustomerRequest", Map.of(
                "type", "object",
                "properties", Map.of(
                        "fullName", Map.of("type", "string"),
                        "email", Map.of("type", "string"),
                        "phone", Map.of("type", "string")
                )
        ));

        schemas.put("UpdateDoctorRequest", Map.of(
                "type", "object",
                "properties", Map.of(
                        "fullName", Map.of("type", "string"),
                        "specialty", Map.of("type", "string"),
                        "active", Map.of("type", "boolean"),
                        "slotMinutes", Map.of("type", "integer")
                )
        ));

        schemas.put("UpdateScheduleRequest", Map.of(
                "type", "object",
                "properties", Map.of(
                        "startTime", Map.of("type", "string"),
                        "endTime", Map.of("type", "string"),
                        "capacity", Map.of("type", "integer")
                )
        ));

        components.put("schemas", schemas);
        components.put("securitySchemes", Map.of(
                "bearerAuth", Map.of(
                        "type", "http",
                        "scheme", "bearer",
                        "bearerFormat", "JWT"
                )
        ));

        return components;
    }

    private Map<String, Object> param(String name, String in, String type) {
        return Map.of("name", name, "in", in, "required", true, "schema", Map.of("type", type, "format", "int64"));
    }

    private Map<String, Object> pathParam(String name, String type) {
        return Map.of("name", name, "in", "path", "required", true, "schema", Map.of("type", type, "format", "int64"));
    }

    private Map<String, Object> stringPathParam(String name) {
        return Map.of("name", name, "in", "path", "required", true, "schema", Map.of("type", "string"));
    }

    private Map<String, Object> jsonBody(String ref) {
        return Map.of("required", true, "content", Map.of(
                "application/json", Map.of("schema", Map.of("$ref", ref))
        ));
    }
}
