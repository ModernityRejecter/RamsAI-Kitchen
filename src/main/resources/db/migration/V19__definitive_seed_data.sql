-- V19__definitive_seed_data.sql
-- Clean up all demo data and insert a highly realistic, definitive dataset.

-- 1. CLEANUP
DELETE FROM ai_messages;
DELETE FROM ai_chat_sessions;
DELETE FROM order_items;
DELETE FROM orders;
DELETE FROM reviews;
DELETE FROM restaurant_tables;
DELETE FROM restaurant_walls;
DELETE FROM audit_logs;
DELETE FROM users WHERE role = 'CUSTOMER';

-- 2. USERS (Real Romanian Names)
INSERT INTO users (username, password_hash, email, role, is_active, is_email_verified, created_at) VALUES
('mihai.popescu', '$2a$10$C0JGX.Yzi1wlaXNcspQC1OYgk7k0q4hY9u0MCnxkUH3tjbFaQ7Wl2', 'mihai.popescu@mail.ro', 'CUSTOMER', true, true, NOW() - INTERVAL '30 days'),
('andrei.ionescu', '$2a$10$C0JGX.Yzi1wlaXNcspQC1OYgk7k0q4hY9u0MCnxkUH3tjbFaQ7Wl2', 'andrei.ionescu@mail.ro', 'CUSTOMER', true, true, NOW() - INTERVAL '28 days'),
('elena.dumitru', '$2a$10$C0JGX.Yzi1wlaXNcspQC1OYgk7k0q4hY9u0MCnxkUH3tjbFaQ7Wl2', 'elena.dumitru@mail.ro', 'CUSTOMER', true, true, NOW() - INTERVAL '25 days'),
('maria.radu', '$2a$10$C0JGX.Yzi1wlaXNcspQC1OYgk7k0q4hY9u0MCnxkUH3tjbFaQ7Wl2', 'maria.radu@mail.ro', 'CUSTOMER', true, true, NOW() - INTERVAL '20 days'),
('ionut.marin', '$2a$10$C0JGX.Yzi1wlaXNcspQC1OYgk7k0q4hY9u0MCnxkUH3tjbFaQ7Wl2', 'ionut.marin@mail.ro', 'CUSTOMER', true, true, NOW() - INTERVAL '15 days'),
('simona.stoica', '$2a$10$C0JGX.Yzi1wlaXNcspQC1OYgk7k0q4hY9u0MCnxkUH3tjbFaQ7Wl2', 'simona.stoica@mail.ro', 'CUSTOMER', true, true, NOW() - INTERVAL '12 days'),
('alexandru.vasile', '$2a$10$C0JGX.Yzi1wlaXNcspQC1OYgk7k0q4hY9u0MCnxkUH3tjbFaQ7Wl2', 'alexandru.vasile@mail.ro', 'CUSTOMER', true, true, NOW() - INTERVAL '10 days'),
('cristina.stan', '$2a$10$C0JGX.Yzi1wlaXNcspQC1OYgk7k0q4hY9u0MCnxkUH3tjbFaQ7Wl2', 'cristina.stan@mail.ro', 'CUSTOMER', true, true, NOW() - INTERVAL '8 days'),
('stefan.dinu', '$2a$10$C0JGX.Yzi1wlaXNcspQC1OYgk7k0q4hY9u0MCnxkUH3tjbFaQ7Wl2', 'stefan.dinu@mail.ro', 'CUSTOMER', true, true, NOW() - INTERVAL '5 days'),
('diana.munteanu', '$2a$10$C0JGX.Yzi1wlaXNcspQC1OYgk7k0q4hY9u0MCnxkUH3tjbFaQ7Wl2', 'diana.munteanu@mail.ro', 'CUSTOMER', true, true, NOW() - INTERVAL '2 days');

-- 3. LAYOUT (Walls & Tables on a 15x15 Grid: 0 to 14)
-- Outer Walls
INSERT INTO restaurant_walls (x_pos, y_pos) SELECT i, 0 FROM generate_series(0, 14) i; -- Top
INSERT INTO restaurant_walls (x_pos, y_pos) SELECT i, 14 FROM generate_series(0, 14) i; -- Bottom
INSERT INTO restaurant_walls (x_pos, y_pos) SELECT 0, i FROM generate_series(1, 13) i; -- Left
INSERT INTO restaurant_walls (x_pos, y_pos) SELECT 14, i FROM generate_series(1, 13) i; -- Right
-- Kitchen partition (Top Right)
INSERT INTO restaurant_walls (x_pos, y_pos) SELECT i, 4 FROM generate_series(10, 13) i;
INSERT INTO restaurant_walls (x_pos, y_pos) SELECT 10, i FROM generate_series(1, 3) i;
-- VIP Room Partition (Bottom Left)
INSERT INTO restaurant_walls (x_pos, y_pos) SELECT i, 10 FROM generate_series(1, 4) i;
INSERT INTO restaurant_walls (x_pos, y_pos) SELECT 4, i FROM generate_series(11, 13) i;

-- Insert 30 Tables correctly spaced
INSERT INTO restaurant_tables (table_number, status, x_pos, y_pos) VALUES
-- Main Hall (16 tables)
(1, 'FREE', 2, 2), (2, 'FREE', 4, 2), (3, 'FREE', 6, 2), (4, 'FREE', 8, 2),
(5, 'FREE', 2, 4), (6, 'FREE', 4, 4), (7, 'FREE', 6, 4), (8, 'FREE', 8, 4),
(9, 'FREE', 2, 6), (10, 'FREE', 4, 6), (11, 'FREE', 6, 6), (12, 'FREE', 8, 6),
(13, 'FREE', 2, 8), (14, 'FREE', 4, 8), (15, 'FREE', 6, 8), (16, 'FREE', 8, 8),
-- VIP Room (5 tables)
(17, 'FREE', 1, 11), (18, 'FREE', 3, 11),
(19, 'FREE', 2, 12),
(20, 'FREE', 1, 13), (21, 'FREE', 3, 13),
-- Terrace (8 tables)
(22, 'FREE', 6, 11), (23, 'FREE', 8, 11), (24, 'FREE', 10, 11), (25, 'FREE', 12, 11),
(26, 'FREE', 6, 13), (27, 'FREE', 8, 13), (28, 'FREE', 10, 13), (29, 'FREE', 12, 13),
-- Near Kitchen (1 table)
(30, 'FREE', 12, 6);

-- Occupy some tables
UPDATE restaurant_tables SET status = 'OCCUPIED' WHERE table_number IN (2, 5, 13, 16, 21, 28);
UPDATE restaurant_tables SET status = 'BILL_REQUESTED' WHERE table_number IN (8, 20, 26, 30);

-- 4. REVIEWS (Balanced: Most products highly rated, ONLY Double Bacon Burger is negative)
DO $$
DECLARE
    u_mihai BIGINT; u_andrei BIGINT; u_elena BIGINT; u_maria BIGINT; u_ionut BIGINT;
    u_simona BIGINT; u_alex BIGINT; u_cristina BIGINT; u_stefan BIGINT; u_diana BIGINT;
    p_cheese BIGINT; p_jalapeno BIGINT; p_bacon BIGINT; p_carbonara BIGINT; p_lava BIGINT; p_lemonade BIGINT;
BEGIN
    SELECT id INTO u_mihai FROM users WHERE username = 'mihai.popescu';
    SELECT id INTO u_andrei FROM users WHERE username = 'andrei.ionescu';
    SELECT id INTO u_elena FROM users WHERE username = 'elena.dumitru';
    SELECT id INTO u_maria FROM users WHERE username = 'maria.radu';
    SELECT id INTO u_ionut FROM users WHERE username = 'ionut.marin';
    SELECT id INTO u_simona FROM users WHERE username = 'simona.stoica';
    SELECT id INTO u_alex FROM users WHERE username = 'alexandru.vasile';
    SELECT id INTO u_cristina FROM users WHERE username = 'cristina.stan';
    SELECT id INTO u_stefan FROM users WHERE username = 'stefan.dinu';
    SELECT id INTO u_diana FROM users WHERE username = 'diana.munteanu';

    SELECT id INTO p_cheese FROM products WHERE name = 'Classic Cheeseburger' LIMIT 1;
    SELECT id INTO p_jalapeno FROM products WHERE name = 'Spicy Jalapeño Burger' LIMIT 1;
    SELECT id INTO p_bacon FROM products WHERE name = 'Double Bacon Burger' LIMIT 1; -- THE NEGATIVE ONE
    SELECT id INTO p_carbonara FROM products WHERE name = 'Classic Carbonara' LIMIT 1;
    SELECT id INTO p_lava FROM products WHERE name = 'Chocolate Lava Cake' LIMIT 1;
    SELECT id INTO p_lemonade FROM products WHERE name = 'House-made Lemonade' LIMIT 1;

    -- Positive Product: Classic Cheeseburger
    INSERT INTO reviews (product_id, user_id, rating, comment, created_at) VALUES
    (p_cheese, u_mihai, 5, 'Cel mai bun cheeseburger pe care l-am mancat in acest oras. Chifla proaspata, carnea suculenta.', NOW() - INTERVAL '10 days'),
    (p_cheese, u_andrei, 5, 'Exceptional! Recomand cu mare drag.', NOW() - INTERVAL '9 days'),
    (p_cheese, u_elena, 4, 'Foarte gustos.', NOW() - INTERVAL '8 days'),
    (p_cheese, u_maria, 5, 'Clasic si perfect executat. Gust autentic.', NOW() - INTERVAL '7 days'),
    (p_cheese, u_ionut, 5, 'Nota 10 pentru calitatea ingredientelor.', NOW() - INTERVAL '5 days');

    -- Positive Product: Classic Carbonara
    INSERT INTO reviews (product_id, user_id, rating, comment, created_at) VALUES
    (p_carbonara, u_simona, 5, 'Paste facute exact ca in Italia! Guanciale delicios.', NOW() - INTERVAL '12 days'),
    (p_carbonara, u_alex, 5, 'Cremoase si pline de savoare. O incantare.', NOW() - INTERVAL '11 days'),
    (p_carbonara, u_cristina, 5, 'Minunat! Cele mai bune paste din oras.', NOW() - INTERVAL '6 days'),
    (p_carbonara, u_stefan, 4, 'Foarte bune.', NOW() - INTERVAL '4 days');

    -- Positive Product: Chocolate Lava Cake
    INSERT INTO reviews (product_id, user_id, rating, comment, created_at) VALUES
    (p_lava, u_diana, 5, 'Desertul perfect. Cald la interior si rece la exterior.', NOW() - INTERVAL '12 days'),
    (p_lava, u_mihai, 5, 'Incredibil de bun! Ciocolata de inalta calitate.', NOW() - INTERVAL '11 days');

    -- Mixed Product: Spicy Jalapeno Burger
    INSERT INTO reviews (product_id, user_id, rating, comment, created_at) VALUES
    (p_jalapeno, u_simona, 4, 'Gustos, dar extrem de iute. Mi-au dat lacrimile!', NOW() - INTERVAL '12 days'),
    (p_jalapeno, u_alex, 5, 'Perfect pentru cine iubeste mancarea picanta.', NOW() - INTERVAL '11 days'),
    (p_jalapeno, u_cristina, 3, 'Cam iute pentru gustul meu, dar carnea e buna.', NOW() - INTERVAL '6 days');

    -- NEGATIVE PRODUCT: Double Bacon Burger (Isolated negativity)
    INSERT INTO reviews (product_id, user_id, rating, comment, created_at) VALUES
    (p_bacon, u_mihai, 1, 'Baconul era complet ars si amar. Am aruncat jumatate din el. Inacceptabil!', NOW() - INTERVAL '14 days'),
    (p_bacon, u_andrei, 2, 'Foarte uscat. Carnea era talpa, clar supragatita. Nu si-a meritat banii.', NOW() - INTERVAL '13 days'),
    (p_bacon, u_elena, 1, 'Am primit comanda complet gresita si rece. Pe deasupra, baconul era zgarci. Groaznic experienta.', NOW() - INTERVAL '10 days'),
    (p_bacon, u_maria, 2, 'Chifla era veche si se sfarama. Un burger care promite mult si nu ofera nimic.', NOW() - INTERVAL '8 days'),
    (p_bacon, u_ionut, 1, 'Mancarea a intarziat o ora, iar cand a ajuns carnea era nefacuta la mijloc. Risc de toxiinfectie!', NOW() - INTERVAL '5 days'),
    (p_bacon, u_diana, 3, 'A fost ok, dar foarte mult ulei. Curgea grasimea din el.', NOW() - INTERVAL '1 days');

    -- Update average ratings
    UPDATE products p SET average_rating = COALESCE((SELECT ROUND(AVG(rating)::numeric, 1) FROM reviews r WHERE r.product_id = p.id), 0);
END $$;

-- 5. ACTIVE ORDERS WITH DETAILED COMMENTS
DO $$
DECLARE
    u_andrei BIGINT; u_maria BIGINT; u_alex BIGINT; u_cristina BIGINT;
    t2 BIGINT; t5 BIGINT; t13 BIGINT; t16 BIGINT;
    o_id BIGINT;
    p_carbonara BIGINT; p_lava BIGINT; p_cheese BIGINT; p_lemonade BIGINT;
BEGIN
    SELECT id INTO u_andrei FROM users WHERE username = 'andrei.ionescu';
    SELECT id INTO u_maria FROM users WHERE username = 'maria.radu';
    SELECT id INTO u_alex FROM users WHERE username = 'alexandru.vasile';
    SELECT id INTO u_cristina FROM users WHERE username = 'cristina.stan';

    SELECT id INTO t2 FROM restaurant_tables WHERE table_number = 2;
    SELECT id INTO t5 FROM restaurant_tables WHERE table_number = 5;
    SELECT id INTO t13 FROM restaurant_tables WHERE table_number = 13;
    SELECT id INTO t16 FROM restaurant_tables WHERE table_number = 16;

    SELECT id INTO p_carbonara FROM products WHERE name = 'Classic Carbonara' LIMIT 1;
    SELECT id INTO p_lava FROM products WHERE name = 'Chocolate Lava Cake' LIMIT 1;
    SELECT id INTO p_cheese FROM products WHERE name = 'Classic Cheeseburger' LIMIT 1;
    SELECT id INTO p_lemonade FROM products WHERE name = 'House-made Lemonade' LIMIT 1;

    -- Order 1: RECEIVED (Table 2)
    INSERT INTO orders (table_id, customer_id, status, total_price, created_at, updated_at, placed_at)
    VALUES (t2, u_andrei, 'RECEIVED', 33.48, NOW() - INTERVAL '5 minutes', NOW() - INTERVAL '5 minutes', NOW() - INTERVAL '5 minutes')
    RETURNING id INTO o_id;
    
    INSERT INTO order_items (order_id, product_id, quantity, unit_price, special_notes, item_status) VALUES 
    (o_id, p_cheese, 2, 12.99, 'Fara ceapa si fara rosii la unul dintre ei, va rog frumos. Copilul este alergic.', 'PENDING'),
    (o_id, p_lemonade, 2, 4.50, 'Fara gheata, la temperatura camerei.', 'PENDING');

    -- Order 2: COOKING (Table 5)
    INSERT INTO orders (table_id, customer_id, status, total_price, created_at, updated_at, placed_at)
    VALUES (t5, u_maria, 'COOKING', 23.98, NOW() - INTERVAL '25 minutes', NOW() - INTERVAL '10 minutes', NOW() - INTERVAL '25 minutes')
    RETURNING id INTO o_id;

    INSERT INTO order_items (order_id, product_id, quantity, unit_price, special_notes, item_status) VALUES 
    (o_id, p_carbonara, 1, 14.99, 'Va rog sa nu folositi piper negru pe deasupra.', 'COOKING'),
    (o_id, p_lava, 1, 8.99, 'Adaugati extra sos de ciocolata daca se poate, platesc diferenta.', 'PENDING');

    -- Order 3: READY (Table 13)
    INSERT INTO orders (table_id, customer_id, status, total_price, created_at, updated_at, placed_at)
    VALUES (t13, u_alex, 'READY', 29.98, NOW() - INTERVAL '40 minutes', NOW() - INTERVAL '2 minutes', NOW() - INTERVAL '40 minutes')
    RETURNING id INTO o_id;

    INSERT INTO order_items (order_id, product_id, quantity, unit_price, special_notes, item_status) VALUES 
    (o_id, p_carbonara, 2, 14.99, 'Baconul din carbonara sa fie foarte crocant.', 'READY');
END $$;
