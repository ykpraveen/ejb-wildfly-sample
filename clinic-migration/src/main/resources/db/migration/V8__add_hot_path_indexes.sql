CREATE INDEX idx_appointments_double_booking ON appointment_mgmt.appointments (clinic_id, doctor_id, schedule_id, appointment_time, status);
CREATE INDEX idx_appointments_daily_cap ON appointment_mgmt.appointments (clinic_id, customer_id, appointment_date, status);

CREATE INDEX idx_doctor_schedules_by_doctor ON schedule_mgmt.doctor_schedules (clinic_id, doctor_id);
CREATE INDEX idx_doctor_schedules_by_doctor_date ON schedule_mgmt.doctor_schedules (clinic_id, doctor_id, available_date);
