CREATE TABLE subscription_payments (
    id BIGSERIAL PRIMARY KEY,
    barber_id BIGINT NOT NULL REFERENCES users (id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    amount NUMERIC(10, 2) NOT NULL,
    tx_id VARCHAR(25),
    period_start DATE,
    period_end DATE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    confirmed_at TIMESTAMP
);

CREATE INDEX idx_subscription_payments_barber_id ON subscription_payments (barber_id);
CREATE INDEX idx_subscription_payments_status ON subscription_payments (status);
