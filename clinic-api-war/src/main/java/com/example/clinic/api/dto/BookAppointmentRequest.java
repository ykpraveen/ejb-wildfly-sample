package com.example.clinic.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class BookAppointmentRequest {
    @NotNull
    public Long clinicId;
    @NotNull
    public Long customerId;
    @NotNull
    public Long doctorId;
    @NotNull
    public Long scheduleId;
    @NotBlank
    public String appointmentTime;
    public String notes;
}
