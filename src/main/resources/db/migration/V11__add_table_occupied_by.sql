-- V9__add_table_occupied_by.sql
ALTER TABLE restaurant_tables
    ADD COLUMN occupied_by_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL;
