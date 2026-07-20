package com.example.clinic.api.dto;

import jakarta.validation.constraints.NotNull;

public class SelectScheduleRequest {
    @NotNull
    public Long scheduleId;
}
