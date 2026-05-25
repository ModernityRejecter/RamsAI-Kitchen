-- V4__add_profile_picture_column.sql
-- Add profile_picture_url column to users table
ALTER TABLE users ADD COLUMN profile_picture_url VARCHAR(1024);
