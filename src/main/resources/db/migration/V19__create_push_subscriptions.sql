CREATE TABLE push_subscriptions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id),
    platform VARCHAR(20) NOT NULL,
    endpoint VARCHAR(500),
    p256dh VARCHAR(255),
    auth_key VARCHAR(255),
    expo_push_token VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_push_subscriptions_endpoint ON push_subscriptions (endpoint) WHERE endpoint IS NOT NULL;
CREATE UNIQUE INDEX idx_push_subscriptions_expo_token ON push_subscriptions (expo_push_token) WHERE expo_push_token IS NOT NULL;
CREATE INDEX idx_push_subscriptions_user_id ON push_subscriptions (user_id);
