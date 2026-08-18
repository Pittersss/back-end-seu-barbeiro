CREATE TABLE appointments (
    id BIGSERIAL PRIMARY KEY,
    scheduled_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(20),
    client_id BIGINT NOT NULL,
    barber_id BIGINT NOT NULL,
    service_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_appointments_client FOREIGN KEY (client_id) REFERENCES users (id),
    CONSTRAINT fk_appointments_barber FOREIGN KEY (barber_id) REFERENCES users (id),
    CONSTRAINT fk_appointments_service FOREIGN KEY (service_id) REFERENCES services (id)
);

CREATE INDEX idx_appointments_client_id ON appointments (client_id);
CREATE INDEX idx_appointments_barber_id ON appointments (barber_id);
