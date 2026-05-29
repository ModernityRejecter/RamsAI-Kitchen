-- Fix hardcoded product ratings by recalculating them from actual reviews
UPDATE products p
SET average_rating = COALESCE((
    SELECT AVG(rating)
    FROM reviews r
    WHERE r.product_id = p.id
), 0.0);
