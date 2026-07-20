CREATE TABLE doctor_schedules (
    id BIGINT NOT NULL AUTO_INCREMENT,
    clinic_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    available_date DATE NOT NULL,
    start_time TIME(6) NOT NULL,
    end_time TIME(6) NOT NULL,
    capacity INT NOT NULL,
    deleted BOOLEAN NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id)
);
