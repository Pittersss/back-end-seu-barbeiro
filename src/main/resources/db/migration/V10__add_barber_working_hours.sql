-- Barber-defined daily working window + optional daily break. Applies to every
-- weekday; one-off exceptions live in the time_blocks table (V11).
ALTER TABLE users ADD COLUMN work_start_hour INTEGER NOT NULL DEFAULT 9;
ALTER TABLE users ADD COLUMN work_end_hour INTEGER NOT NULL DEFAULT 18;
ALTER TABLE users ADD COLUMN break_start_hour INTEGER;
ALTER TABLE users ADD COLUMN break_end_hour INTEGER;
