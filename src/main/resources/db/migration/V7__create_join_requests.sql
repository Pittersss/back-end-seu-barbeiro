CREATE TABLE join_requests (
    id BIGSERIAL PRIMARY KEY,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    message VARCHAR(500),
    barber_id BIGINT NOT NULL,
    barber_shop_id BIGINT NOT NULL,
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_join_requests_barber FOREIGN KEY (barber_id) REFERENCES users (id),
    CONSTRAINT fk_join_requests_barber_shop FOREIGN KEY (barber_shop_id) REFERENCES barber_shops (id)
);

CREATE INDEX idx_join_requests_barber_shop_id ON join_requests (barber_shop_id);
CREATE INDEX idx_join_requests_barber_id ON join_requests (barber_id);
