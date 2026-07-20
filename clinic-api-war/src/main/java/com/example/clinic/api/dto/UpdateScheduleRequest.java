package com.example.clinic.api.dto;

import jakarta.validation.constraints.Positive;

public class UpdateScheduleRequest {
    public String startTime;
    public String endTime;
    @Positive
    public Integer capacity;
}
