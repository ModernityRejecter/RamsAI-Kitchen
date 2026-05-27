DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_product_ingredient_product_id_ingredient_id'
    ) THEN
        ALTER TABLE product_ingredients
            ADD CONSTRAINT uk_product_ingredient_product_id_ingredient_id
            UNIQUE (product_id, ingredient_id);
    END IF;
END $$;
