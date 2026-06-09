-- V19's "definitive" cleanup runs DELETE FROM users WHERE role = 'CUSTOMER',
-- which also removes the predefined 'customer' login seeded in V16 and never
-- re-adds it. Re-seed it here so it survives both existing and fresh databases
-- (this runs after V19). Password: RamsAI@2026!
INSERT INTO users (username, password_hash, email, role, is_active, is_email_verified, created_at)
VALUES ('customer', '$2a$10$C0JGX.Yzi1wlaXNcspQC1OYgk7k0q4hY9u0MCnxkUH3tjbFaQ7Wl2', 'customer@ramsai.com', 'CUSTOMER', true, true, NOW())
ON CONFLICT (username) DO NOTHING;
