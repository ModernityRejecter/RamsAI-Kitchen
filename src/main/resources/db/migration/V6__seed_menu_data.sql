-- V3__seed_menu_data.sql
-- Seed data for Categories and Products

-- 1. Insert Categories
INSERT INTO categories (name, description) VALUES 
('Burgers', 'Juicy, hand-crafted burgers made with premium beef.'),
('Pasta', 'Authentic Italian pasta dishes with fresh sauces.'),
('Desserts', 'Sweet treats to end your meal perfectly.'),
('Drinks', 'Refreshing beverages and house-made specialties.');

-- 2. Insert Products
-- Burgers
INSERT INTO products (category_id, name, description, base_price, is_active, is_special_offer, discount_price, average_rating, approval_status) 
VALUES 
(1, 'Classic Cheeseburger', 'Angus beef patty, cheddar cheese, lettuce, tomato, and our secret sauce.', 12.99, true, false, null, 4.8, 'APPROVED'),
(1, 'Spicy Jalapeño Burger', 'Beef patty with pepper jack cheese, jalapeños, and spicy mayo.', 13.50, true, true, 10.99, 4.5, 'APPROVED'),
(1, 'Double Bacon Burger', 'Two patties, four slices of crispy bacon, and BBQ sauce.', 15.99, true, false, null, 4.9, 'APPROVED');

-- Pasta
INSERT INTO products (category_id, name, description, base_price, is_active, is_special_offer, discount_price, average_rating, approval_status) 
VALUES 
(2, 'Classic Carbonara', 'Spaghetti with guanciale, pecorino romano, egg yolk, and black pepper.', 14.99, true, false, null, 4.7, 'APPROVED'),
(2, 'Truffle Mushroom Pasta', 'Fettuccine with wild mushrooms and creamy truffle sauce.', 18.50, true, true, 15.99, 4.9, 'APPROVED');

-- Desserts
INSERT INTO products (category_id, name, description, base_price, is_active, is_special_offer, discount_price, average_rating, approval_status) 
VALUES 
(3, 'Chocolate Lava Cake', 'Warm chocolate cake with a molten center, served with vanilla ice cream.', 8.99, true, false, null, 4.9, 'APPROVED'),
(3, 'New York Cheesecake', 'Classic creamy cheesecake with strawberry coulis.', 7.50, true, false, null, 4.6, 'APPROVED');

-- Drinks
INSERT INTO products (category_id, name, description, base_price, is_active, is_special_offer, discount_price, average_rating, approval_status) 
VALUES 
(4, 'House-made Lemonade', 'Freshly squeezed lemons with a hint of mint.', 4.50, true, false, null, 4.4, 'APPROVED'),
(4, 'Iced Caramel Macchiato', 'Rich espresso with milk and sweet caramel syrup.', 5.50, true, true, 4.00, 4.7, 'APPROVED');
