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
);
