CREATE TABLE barbershop_requests (
    id BIGSERIAL PRIMARY KEY,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    shop_name VARCHAR(255) NOT NULL,
    shop_address VARCHAR(255),
    shop_phone VARCHAR(30),
    requester_id BIGINT NOT NULL,
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_barbershop_requests_requester FOREIGN KEY (requester_id) REFERENCES users (id)
);

CREATE INDEX idx_barbershop_requests_status ON barbershop_requests (status);
