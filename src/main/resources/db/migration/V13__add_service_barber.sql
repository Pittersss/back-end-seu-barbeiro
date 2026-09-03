-- Lets any barber (not just the shop owner) own the services they perform.
-- Nullable because existing rows predate this and had no assigned barber.
ALTER TABLE services ADD COLUMN barber_id BIGINT REFERENCES users (id);

CREATE INDEX idx_services_barber_id ON services (barber_id);
