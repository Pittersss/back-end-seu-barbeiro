-- Before V13, only the shop owner could ever create a service, so every
-- pre-existing row with a null barber_id was in practice the owner's.
-- Backfill it so those legacy services show a barber and remain manageable
-- through the same owner/assigned-barber rule as newly created ones.
UPDATE services
SET barber_id = (
    SELECT owner_id
    FROM barber_shops
    WHERE barber_shops.id = services.barber_shop_id
)
WHERE barber_id IS NULL;
