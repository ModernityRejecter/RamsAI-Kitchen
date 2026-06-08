-- V17__seed_demo_orders.sql
-- Demo sales history so the manager "Sales Intelligence" dashboard (User Story 10)
-- has trends to visualise: ~28 days of SERVED orders with line items, plus a few
-- reviews so the average-rating visuals are populated.
--
-- Postgres-only (the test suite runs on H2 with Flyway disabled).
-- Guarded: it no-ops if real order data already exists, and references live
-- product ids/prices so revenue figures stay correct.

DO $$
DECLARE
    cust_id      BIGINT;
    tbl_id       BIGINT;
    existing     BIGINT;
    prod_ids     BIGINT[];
    prod_n       INT;
    d            INT;
    n            INT;
    i            INT;
    orders_today INT;
    lines_n      INT;
    new_id       BIGINT;
    ts           TIMESTAMP;
    pick         BIGINT;
    qty          INT;
BEGIN
    SELECT COUNT(*) INTO existing FROM orders WHERE status <> 'DRAFT';
    IF existing > 25 THEN
        RAISE NOTICE 'Demo order seed skipped: % non-draft orders already present.', existing;
        RETURN;
    END IF;

    SELECT id INTO cust_id FROM users WHERE role = 'CUSTOMER' ORDER BY id LIMIT 1;
    IF cust_id IS NULL THEN
        RAISE NOTICE 'Demo order seed skipped: no CUSTOMER user found.';
        RETURN;
    END IF;

    -- nullable: fine if the restaurant has no tables seeded yet
    SELECT id INTO tbl_id FROM restaurant_tables ORDER BY id LIMIT 1;

    SELECT array_agg(id ORDER BY id) INTO prod_ids FROM products WHERE approval_status = 'APPROVED';
    prod_n := COALESCE(array_length(prod_ids, 1), 0);
    IF prod_n = 0 THEN
        RAISE NOTICE 'Demo order seed skipped: no approved products.';
        RETURN;
    END IF;

    FOR d IN 0..27 LOOP
        orders_today := 2 + ((d * 7 + 3) % 6);          -- 2..7 orders per day
        FOR n IN 1..orders_today LOOP
            ts := (CURRENT_DATE - make_interval(days => d))
                  + make_interval(hours => 11 + (n % 11), mins => (n * 17 + d * 11) % 60);

            INSERT INTO orders (table_id, customer_id, status, total_price, created_at, updated_at, placed_at)
            VALUES (tbl_id, cust_id, 'SERVED', 0, ts, ts, ts)
            RETURNING id INTO new_id;

            lines_n := 1 + ((n + d) % 3);               -- 1..3 line items per order
            FOR i IN 1..lines_n LOOP
                pick := prod_ids[1 + ((n * 3 + i * 5 + d * 2) % prod_n)];
                qty  := 1 + ((i + n + d) % 3);          -- 1..3 units per line
                INSERT INTO order_items (order_id, product_id, quantity, unit_price, item_status)
                SELECT new_id, p.id, qty, p.base_price, 'SERVED'
                FROM products p WHERE p.id = pick;
            END LOOP;

            UPDATE orders o
            SET total_price = COALESCE((SELECT SUM(oi.quantity * oi.unit_price)
                                        FROM order_items oi WHERE oi.order_id = o.id), 0)
            WHERE o.id = new_id;
        END LOOP;
    END LOOP;

    -- One review per approved product (alternating 4/5 stars) so ratings show up.
    INSERT INTO reviews (product_id, user_id, rating, comment, created_at)
    SELECT p.id, cust_id, 4 + (p.id % 2)::int, 'Solid choice, would order again.',
           CURRENT_DATE - make_interval(days => (p.id % 14)::int)
    FROM products p
    WHERE p.approval_status = 'APPROVED'
      AND NOT EXISTS (SELECT 1 FROM reviews r WHERE r.product_id = p.id AND r.user_id = cust_id);

    RAISE NOTICE 'Demo orders and reviews seeded.';
END $$;
