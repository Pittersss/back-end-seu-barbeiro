CREATE TABLE services (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    image BYTEA,
    duration_minutes INTEGER,
    price NUMERIC(10, 2) NOT NULL,
    available BOOLEAN NOT NULL DEFAULT true,
    barber_shop_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_services_barber_shop FOREIGN KEY (barber_shop_id) REFERENCES barber_shops (id)
);

CREATE INDEX idx_services_barber_shop_id ON services (barber_shop_id);
