ALTER TABLE users
    ADD COLUMN blocked_from_owning BOOLEAN NOT NULL DEFAULT false;
