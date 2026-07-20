package com.example.clinic.schedule;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

import java.time.LocalDate;
import java.time.LocalTime;

@Singleton
@Startup
public class ScheduleSeedData {
    @jakarta.inject.Inject
    private ScheduleManagementService scheduleManagementService;

    @PostConstruct
    public void seed() {
        try {
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            LocalDate dayAfter = LocalDate.now().plusDays(2);

            scheduleManagementService.addSchedule(1L, 1L, tomorrow,
                    LocalTime.of(9, 0), LocalTime.of(12, 0), 5);

            scheduleManagementService.addSchedule(1L, 1L, tomorrow,
                    LocalTime.of(14, 0), LocalTime.of(17, 0), 5);

            scheduleManagementService.addSchedule(1L, 2L, tomorrow,
                    LocalTime.of(8, 0), LocalTime.of(12, 0), 8);

            scheduleManagementService.addSchedule(1L, 1L, dayAfter,
                    LocalTime.of(9, 0), LocalTime.of(12, 0), 5);

            scheduleManagementService.addSchedule(1L, 2L, dayAfter,
                    LocalTime.of(13, 0), LocalTime.of(17, 0), 8);
        } catch (Exception ignored) {
        }
    }
}
