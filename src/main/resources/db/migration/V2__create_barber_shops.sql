CREATE TABLE barber_shops (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(255),
    phone VARCHAR(30),
    accepting_barbers BOOLEAN NOT NULL DEFAULT true,
    owner_id BIGINT UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_barber_shops_owner FOREIGN KEY (owner_id) REFERENCES users (id)
);

ALTER TABLE users
    ADD CONSTRAINT fk_users_barber_shop FOREIGN KEY (barber_shop_id) REFERENCES barber_shops (id);
