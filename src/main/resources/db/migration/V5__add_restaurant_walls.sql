-- V5__add_restaurant_walls.sql
CREATE TABLE restaurant_walls (
    id BIGSERIAL PRIMARY KEY,
    x_pos INTEGER NOT NULL,
    y_pos INTEGER NOT NULL
);
