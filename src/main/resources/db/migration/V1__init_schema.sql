-- V1__init_schema.sql
-- Initial schema for Ramsai Kitchen

-- 1. Categories
CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT
);

-- 2. Ingredients
CREATE TABLE ingredients (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    unit VARCHAR(50) NOT NULL,
    current_stock DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    minimum_stock_threshold DOUBLE PRECISION NOT NULL DEFAULT 0.0
);

-- 3. Products
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL REFERENCES categories(id),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    base_price DECIMAL(19, 2) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_special_offer BOOLEAN NOT NULL DEFAULT FALSE,
    discount_price DECIMAL(19, 2),
    average_rating DOUBLE PRECISION DEFAULT 0.0,
    approval_status VARCHAR(50) NOT NULL DEFAULT 'PENDING'
);

-- 4. Product Ingredients (Join Table for Recipes)
CREATE TABLE product_ingredients (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    ingredient_id BIGINT NOT NULL REFERENCES ingredients(id) ON DELETE CASCADE,
    quantity_required DOUBLE PRECISION NOT NULL
);

-- 5. Users
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    role VARCHAR(50) NOT NULL
);

-- 6. Reviews
CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 7. Restaurant Tables
CREATE TABLE restaurant_tables (
    id BIGSERIAL PRIMARY KEY,
    table_number INTEGER NOT NULL UNIQUE,
    status VARCHAR(50) NOT NULL DEFAULT 'FREE',
    x_pos INTEGER,
    y_pos INTEGER
);

-- 8. Orders
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    table_id BIGINT REFERENCES restaurant_tables(id),
    customer_id BIGINT,
    status VARCHAR(50) NOT NULL DEFAULT 'RECEIVED',
    total_price DECIMAL(19, 2) NOT NULL DEFAULT 0.0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 9. Order Items
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id),
    quantity INTEGER NOT NULL DEFAULT 1,
    unit_price DECIMAL(19, 2) NOT NULL,
    special_notes TEXT,
    item_status VARCHAR(50) NOT NULL DEFAULT 'PENDING'
);

-- 10. AI Chat Sessions
CREATE TABLE ai_chat_sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    summary TEXT
);

-- 11. AI Messages
CREATE TABLE ai_messages (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES ai_chat_sessions(id) ON DELETE CASCADE,
    sender_type VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
