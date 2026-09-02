CREATE TABLE client_blocks (
    id BIGSERIAL PRIMARY KEY,
    barber_id BIGINT NOT NULL,
    client_id BIGINT NOT NULL,
    reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_client_blocks_barber FOREIGN KEY (barber_id) REFERENCES users (id),
    CONSTRAINT fk_client_blocks_client FOREIGN KEY (client_id) REFERENCES users (id),
    CONSTRAINT uq_client_blocks_barber_client UNIQUE (barber_id, client_id)
);

CREATE INDEX idx_client_blocks_barber_id ON client_blocks (barber_id);
