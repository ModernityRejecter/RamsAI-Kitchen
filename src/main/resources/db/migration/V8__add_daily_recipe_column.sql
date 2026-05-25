-- V6__add_daily_recipe_column.sql
ALTER TABLE products ADD COLUMN is_daily_recipe BOOLEAN NOT NULL DEFAULT FALSE;

-- Mark some items as daily recipes
UPDATE products SET is_daily_recipe = TRUE WHERE name IN ('Classic Carbonara', 'Spicy Jalapeño Burger', 'Chocolate Lava Cake');
