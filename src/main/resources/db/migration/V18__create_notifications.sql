CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    recipient_id BIGINT NOT NULL REFERENCES users (id),
    type VARCHAR(30) NOT NULL,
    message VARCHAR(255) NOT NULL,
    appointment_id BIGINT REFERENCES appointments (id),
    read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_recipient_id ON notifications (recipient_id);
CREATE INDEX idx_notifications_recipient_unread ON notifications (recipient_id, read);
