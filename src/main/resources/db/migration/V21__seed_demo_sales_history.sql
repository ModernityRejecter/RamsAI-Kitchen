-- V21__seed_demo_sales_history.sql
-- V19's "definitive" cleanup wiped every historical order, leaving only a handful
-- of live/active orders, so the manager Sales Intelligence dashboard (User Story 10)
-- has nothing to trend. Re-seed ~30 days of completed (SERVED) sales spread across
-- the demo customers, tables and approved products.
--
-- Postgres-only (the test suite runs on H2 with Flyway disabled). Guarded so it
-- no-ops once a real sales history exists, and references live product ids/prices so
-- revenue figures stay correct.
-- NOTE: ManagerAnalyticsService.buildDailySales() keys the daily series off
-- orders.created_at, so every order is backdated (created_at = placed_at) to its day.

DO $$
DECLARE
    cust_ids     BIGINT[];
    cust_n       INT;
    tbl_ids      BIGINT[];
    tbl_n        INT;
    prod_ids     BIGINT[];
    prod_prices  NUMERIC[];
    prod_n       INT;
    existing     BIGINT;
    d            INT;
    n            INT;
    i            INT;
    dow          INT;
    orders_today INT;
    lines_n      INT;
    new_id       BIGINT;
    ts           TIMESTAMP;
    pick         INT;
    qty          INT;
    seq          INT := 0;
BEGIN
    SELECT COUNT(*) INTO existing FROM orders WHERE status = 'SERVED';
    IF existing > 50 THEN
        RAISE NOTICE 'Sales-history seed skipped: % SERVED orders already present.', existing;
        RETURN;
    END IF;

    SELECT array_agg(id ORDER BY id) INTO cust_ids FROM users WHERE role = 'CUSTOMER';
    cust_n := COALESCE(array_length(cust_ids, 1), 0);
    IF cust_n = 0 THEN
        RAISE NOTICE 'Sales-history seed skipped: no CUSTOMER users.';
        RETURN;
    END IF;

    -- nullable: fine if the restaurant has no tables seeded yet
    SELECT array_agg(id ORDER BY id) INTO tbl_ids FROM restaurant_tables;
    tbl_n := COALESCE(array_length(tbl_ids, 1), 0);

    -- Seed sales only across the canonical V6 menu, so ad-hoc/test products
    -- (e.g. one-off items added through the app) never distort the charts.
    SELECT array_agg(id ORDER BY id), array_agg(base_price ORDER BY id)
      INTO prod_ids, prod_prices
      FROM products
     WHERE approval_status = 'APPROVED' AND is_active = true
       AND name IN ('Classic Cheeseburger', 'Spicy Jalapeño Burger', 'Double Bacon Burger',
                    'Classic Carbonara', 'Truffle Mushroom Pasta', 'Chocolate Lava Cake',
                    'New York Cheesecake', 'House-made Lemonade', 'Iced Caramel Macchiato');
    prod_n := COALESCE(array_length(prod_ids, 1), 0);
    IF prod_n = 0 THEN
        RAISE NOTICE 'Sales-history seed skipped: no canonical menu products.';
        RETURN;
    END IF;

    FOR d IN 0..29 LOOP
        dow := EXTRACT(DOW FROM (CURRENT_DATE - d))::int;   -- 0 Sun .. 6 Sat
        orders_today := 8
            + CASE WHEN dow IN (5, 6) THEN 6 WHEN dow = 0 THEN 3 ELSE 0 END  -- busier weekends
            + ((29 - d) / 8)                                                 -- gentle upward trend
            + ((d * 13 + 7) % 4);                                           -- deterministic jitter 0..3

        FOR n IN 1..orders_today LOOP
            seq := seq + 1;
            ts := (CURRENT_DATE - d)
                  + make_interval(hours => 11 + (seq % 11),
                                  mins  => (seq * 17) % 60,
                                  secs  => (seq * 7) % 60);

            INSERT INTO orders (table_id, customer_id, status, total_price, created_at, updated_at, placed_at)
            VALUES (
                CASE WHEN tbl_n > 0 THEN tbl_ids[1 + (seq % tbl_n)] ELSE NULL END,
                cust_ids[1 + (seq % cust_n)],
                'SERVED', 0, ts, ts, ts)
            RETURNING id INTO new_id;

            lines_n := 1 + (seq % 4);   -- 1..4 line items per order
            FOR i IN 1..lines_n LOOP
                pick := 1 + ((seq * 3 + i * 5) % prod_n);
                qty := 1 + ((seq + i) % 3);   -- 1..3 units per line

                INSERT INTO order_items (order_id, product_id, quantity, unit_price, item_status)
                VALUES (new_id, prod_ids[pick], qty, prod_prices[pick], 'SERVED');
            END LOOP;

            UPDATE orders o
               SET total_price = COALESCE((SELECT SUM(oi.quantity * oi.unit_price)
                                           FROM order_items oi WHERE oi.order_id = o.id), 0)
             WHERE o.id = new_id;
        END LOOP;
    END LOOP;

    RAISE NOTICE 'Sales-history seed complete: % SERVED orders inserted across 30 days.', seq;
END $$;
