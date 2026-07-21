CREATE TABLE appointment_mgmt.appointments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    clinic_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    schedule_id BIGINT NOT NULL,
    appointment_date DATE NOT NULL,
    appointment_time TIME(6) NOT NULL,
    status VARCHAR(20) NOT NULL,
    notes VARCHAR(255),
    created_by VARCHAR(80) NOT NULL,
    deleted BOOLEAN NOT NULL,
    reschedule_count INT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id)
);
