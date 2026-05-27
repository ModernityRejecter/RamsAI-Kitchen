-- Seed ingredients and recipe composition for existing products.

INSERT INTO ingredients (name, unit, current_stock, minimum_stock_threshold) VALUES
('Beef Patty', 'pcs', 120, 30),
('Burger Bun', 'pcs', 150, 40),
('Cheddar Cheese Slice', 'pcs', 180, 40),
('Lettuce', 'g', 6000, 1200),
('Tomato', 'g', 5000, 1000),
('Jalapeño', 'g', 1500, 300),
('Bacon', 'g', 2500, 600),
('Spaghetti', 'g', 9000, 1800),
('Fettuccine', 'g', 8000, 1600),
('Egg Yolk', 'pcs', 300, 80),
('Pecorino Romano', 'g', 2200, 450),
('Guanciale', 'g', 2800, 600),
('Wild Mushrooms', 'g', 3600, 800),
('Truffle Cream Sauce', 'ml', 2600, 500),
('Chocolate', 'g', 3500, 700),
('Vanilla Ice Cream', 'g', 4200, 900),
('Cream Cheese', 'g', 4500, 900),
('Strawberry Coulis', 'ml', 2400, 500),
('Lemon Juice', 'ml', 5200, 1000),
('Mint', 'g', 800, 200),
('Espresso Shot', 'pcs', 260, 60),
('Milk', 'ml', 9000, 1800),
('Caramel Syrup', 'ml', 2600, 500);

INSERT INTO product_ingredients (product_id, ingredient_id, quantity_required)
SELECT p.id, i.id, v.quantity_required
FROM (
    VALUES
        ('Classic Cheeseburger', 'Beef Patty', 1.0),
        ('Classic Cheeseburger', 'Burger Bun', 1.0),
        ('Classic Cheeseburger', 'Cheddar Cheese Slice', 1.0),
        ('Classic Cheeseburger', 'Lettuce', 20.0),
        ('Classic Cheeseburger', 'Tomato', 30.0),

        ('Spicy Jalapeño Burger', 'Beef Patty', 1.0),
        ('Spicy Jalapeño Burger', 'Burger Bun', 1.0),
        ('Spicy Jalapeño Burger', 'Cheddar Cheese Slice', 1.0),
        ('Spicy Jalapeño Burger', 'Jalapeño', 25.0),
        ('Spicy Jalapeño Burger', 'Tomato', 20.0),

        ('Double Bacon Burger', 'Beef Patty', 2.0),
        ('Double Bacon Burger', 'Burger Bun', 1.0),
        ('Double Bacon Burger', 'Bacon', 60.0),
        ('Double Bacon Burger', 'Cheddar Cheese Slice', 2.0),

        ('Classic Carbonara', 'Spaghetti', 120.0),
        ('Classic Carbonara', 'Guanciale', 40.0),
        ('Classic Carbonara', 'Egg Yolk', 2.0),
        ('Classic Carbonara', 'Pecorino Romano', 25.0),

        ('Truffle Mushroom Pasta', 'Fettuccine', 130.0),
        ('Truffle Mushroom Pasta', 'Wild Mushrooms', 90.0),
        ('Truffle Mushroom Pasta', 'Truffle Cream Sauce', 80.0),

        ('Chocolate Lava Cake', 'Chocolate', 70.0),
        ('Chocolate Lava Cake', 'Vanilla Ice Cream', 50.0),

        ('New York Cheesecake', 'Cream Cheese', 90.0),
        ('New York Cheesecake', 'Strawberry Coulis', 20.0),

        ('House-made Lemonade', 'Lemon Juice', 120.0),
        ('House-made Lemonade', 'Mint', 4.0),

        ('Iced Caramel Macchiato', 'Espresso Shot', 1.0),
        ('Iced Caramel Macchiato', 'Milk', 180.0),
        ('Iced Caramel Macchiato', 'Caramel Syrup', 30.0)
) AS v(product_name, ingredient_name, quantity_required)
JOIN products p ON p.name = v.product_name
JOIN ingredients i ON i.name = v.ingredient_name;
