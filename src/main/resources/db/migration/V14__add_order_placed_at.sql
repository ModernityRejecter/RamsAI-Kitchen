ALTER TABLE orders
    ADD COLUMN placed_at TIMESTAMP;

UPDATE orders
SET placed_at = created_at
WHERE placed_at IS NULL AND status <> 'DRAFT';
