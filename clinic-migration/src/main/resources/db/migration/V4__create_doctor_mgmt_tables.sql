CREATE TABLE doctor_mgmt.doctors (
    id BIGINT NOT NULL AUTO_INCREMENT,
    clinic_id BIGINT NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    username VARCHAR(80) NOT NULL,
    specialty VARCHAR(80) NOT NULL,
    active BOOLEAN NOT NULL,
    deleted BOOLEAN NOT NULL,
    slot_minutes INT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_doctors_clinic_username UNIQUE (clinic_id, username)
);
