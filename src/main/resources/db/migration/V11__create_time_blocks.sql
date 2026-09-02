CREATE TABLE time_blocks (
    id BIGSERIAL PRIMARY KEY,
    starts_at TIMESTAMP NOT NULL,
    ends_at TIMESTAMP NOT NULL,
    reason VARCHAR(255),
    barber_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_time_blocks_barber FOREIGN KEY (barber_id) REFERENCES users (id)
);

CREATE INDEX idx_time_blocks_barber_id ON time_blocks (barber_id);
