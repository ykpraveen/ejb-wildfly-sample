package com.example.clinic.api.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateAppointmentStatusRequest {
    @NotBlank
    public String status;
}
