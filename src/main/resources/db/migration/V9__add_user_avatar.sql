-- Profile pictures for every user (client, barber, admin). Stored inline as a
-- base64 data string; the mobile client downscales to ~256px JPEG before upload,
-- so rows stay small. `phone` already exists on this single-inheritance table.
ALTER TABLE users ADD COLUMN avatar_base64 TEXT;
