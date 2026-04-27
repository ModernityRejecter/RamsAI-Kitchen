CREATE TABLE inventory_logs (
    id BIGSERIAL PRIMARY KEY,
    ingredient_id BIGINT NOT NULL,
    change_amount DOUBLE PRECISION NOT NULL,
    reason VARCHAR(50) NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inventory_logs_ingredient FOREIGN KEY (ingredient_id) REFERENCES ingredients (id)
);
