-- AI Chat Tables
CREATE TABLE ai_chat_sessions (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL, -- Chef or Manager
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    summary TEXT
);

CREATE TABLE ai_messages (
    id SERIAL PRIMARY KEY,
    session_id INT REFERENCES ai_chat_sessions(id) ON DELETE CASCADE,
    sender_type VARCHAR(10) NOT NULL, -- 'USER' or 'AI'
    content TEXT NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
-- Create Enums
CREATE TYPE table_status AS ENUM ('FREE', 'OCCUPIED', 'BILL_REQUESTED');
CREATE TYPE order_status AS ENUM ('RECEIVED', 'COOKING', 'READY', 'SERVED', 'CANCELLED');
CREATE TYPE item_status AS ENUM ('PENDING', 'PREPARING', 'READY', 'SERVED');

-- Tables
CREATE TABLE restaurant_tables (
    id SERIAL PRIMARY KEY,
    table_number INT UNIQUE NOT NULL,
    status table_status DEFAULT 'FREE',
    x_pos INT,
    y_pos INT
);

CREATE TABLE orders (
    id SERIAL PRIMARY KEY,
    table_id INT REFERENCES restaurant_tables(id),
    customer_id INT NULL,
    status order_status DEFAULT 'RECEIVED',
    total_price DECIMAL(10, 2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_items (
    id SERIAL PRIMARY KEY,
    order_id INT REFERENCES orders(id) ON DELETE CASCADE,
    product_id INT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    unit_price DECIMAL(10, 2) NOT NULL,
    special_notes TEXT,
    item_status item_status DEFAULT 'PENDING'
);
