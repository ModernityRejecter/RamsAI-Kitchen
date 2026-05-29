-- Seed default users for each role
-- Password for all seeded users is 'RamsAI@2026!'
-- Hashed using BCrypt (strength 10)
-- Meets policy: 8+ chars, digit, lower, upper, special, no whitespace
DELETE FROM users WHERE username IN ('manager', 'chef', 'waiter', 'customer');

INSERT INTO users (username, password_hash, email, role, is_active, is_email_verified, created_at)
VALUES 
('manager', '$2a$10$C0JGX.Yzi1wlaXNcspQC1OYgk7k0q4hY9u0MCnxkUH3tjbFaQ7Wl2', 'manager@ramsai.com', 'MANAGER', true, true, NOW()),
('chef', '$2a$10$C0JGX.Yzi1wlaXNcspQC1OYgk7k0q4hY9u0MCnxkUH3tjbFaQ7Wl2', 'chef@ramsai.com', 'CHEF', true, true, NOW()),
('waiter', '$2a$10$C0JGX.Yzi1wlaXNcspQC1OYgk7k0q4hY9u0MCnxkUH3tjbFaQ7Wl2', 'waiter@ramsai.com', 'WAITER', true, true, NOW()),
('customer', '$2a$10$C0JGX.Yzi1wlaXNcspQC1OYgk7k0q4hY9u0MCnxkUH3tjbFaQ7Wl2', 'customer@ramsai.com', 'CUSTOMER', true, true, NOW());
