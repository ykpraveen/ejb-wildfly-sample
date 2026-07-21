CREATE TABLE customer_mgmt.customers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    clinic_id BIGINT NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    username VARCHAR(80) NOT NULL,
    email VARCHAR(160) NOT NULL,
    phone VARCHAR(30),
    deleted BOOLEAN NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_customers_clinic_username UNIQUE (clinic_id, username),
    CONSTRAINT uk_customers_clinic_email UNIQUE (clinic_id, email)
);
