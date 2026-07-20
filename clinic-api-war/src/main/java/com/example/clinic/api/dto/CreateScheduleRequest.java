package com.example.clinic.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class CreateScheduleRequest {
    @NotNull
    public Long clinicId;
    @NotNull
    public Long doctorId;
    @NotBlank
    public String availableDate;
    @NotBlank
    public String startTime;
    @NotBlank
    public String endTime;
    @Positive
    public int capacity;
}
