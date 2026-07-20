package com.example.clinic.api.dto;

import jakarta.validation.constraints.NotNull;

public class SelectDoctorRequest {
    @NotNull
    public Long doctorId;
}
