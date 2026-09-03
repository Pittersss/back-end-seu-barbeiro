-- Same convention as users.avatar_base64: inline base64, downscaled client-side.
ALTER TABLE barber_shops ADD COLUMN photo_base64 TEXT;
