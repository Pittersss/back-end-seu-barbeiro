CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    user_type VARCHAR(31) NOT NULL,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    phone VARCHAR(30),
    pix_key VARCHAR(255),
    available BOOLEAN,
    delay_tolerance INTEGER,
    barber_shop_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_barber_shop_id ON users (barber_shop_id);
