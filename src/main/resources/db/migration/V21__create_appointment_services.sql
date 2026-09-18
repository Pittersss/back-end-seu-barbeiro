CREATE TABLE appointment_services (
    appointment_id BIGINT NOT NULL REFERENCES appointments (id),
    service_id BIGINT NOT NULL REFERENCES services (id),
    PRIMARY KEY (appointment_id, service_id)
);

INSERT INTO appointment_services (appointment_id, service_id)
SELECT id, service_id FROM appointments WHERE service_id IS NOT NULL;
