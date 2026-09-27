-- Bookings are auto-confirmed now; promote anything still waiting on the old manual step.
UPDATE appointments SET status = 'CONFIRMED' WHERE status = 'PENDING';
