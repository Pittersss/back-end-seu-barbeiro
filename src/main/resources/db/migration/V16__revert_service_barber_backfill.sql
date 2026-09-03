-- V15 assumed every legacy null-barber service belonged solely to the shop
-- owner. That was wrong: pre-V13 services were shared across the whole
-- team (any barber could perform them) -- there was simply no per-service
-- assignment yet. Revert them back to barber_id = NULL, which the
-- application treats as "offered by every barber of the shop", so a
-- barber's other services no longer vanish when they're picked for
-- booking. Services explicitly assigned to one barber since V13 are
-- unaffected, since none had been assigned to a shop's owner yet when
-- V15 ran.
UPDATE services
SET barber_id = NULL
WHERE barber_id = (
    SELECT owner_id
    FROM barber_shops
    WHERE barber_shops.id = services.barber_shop_id
);
