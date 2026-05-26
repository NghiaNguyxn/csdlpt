CREATE TABLE category (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE product_basic (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(15,2) NOT NULL CHECK (price >= 0),
    category_id INT REFERENCES category(id),
    is_active BOOLEAN DEFAULT TRUE
);

CREATE TABLE product_detail (
    product_id INT PRIMARY KEY REFERENCES product_basic(id),
    description TEXT
);

CREATE TABLE site (
    id SERIAL PRIMARY KEY,
    site_code VARCHAR(10) UNIQUE NOT NULL, -- HN, DN, HCM
    site_name VARCHAR(100),
    ip_address VARCHAR(50),
    description TEXT
);

CREATE TABLE warehouse (
    id SERIAL PRIMARY KEY,
    code VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    location VARCHAR(100),
    region VARCHAR(50),
    site_id INT REFERENCES site(id)
);

CREATE TABLE inventory (
    warehouse_id INT REFERENCES warehouse(id),
    product_id INT REFERENCES product_basic(id),
    quantity INT NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    reserved_quantity INT NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    PRIMARY KEY (warehouse_id, product_id)
);

CREATE TABLE customer_identity (
    id BIGINT PRIMARY KEY,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL,
    main_site_id INT REFERENCES site(id) NOT NULL,
    UNIQUE (id, main_site_id)
);

CREATE TABLE customer_profile (
    id BIGINT PRIMARY KEY REFERENCES customer_identity(id),
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    address TEXT
);

CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    customer_id BIGINT REFERENCES customer_identity(id),
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'COMPLETED', 'CANCELLED')),
    site_id INT REFERENCES site(id) -- site tạo đơn
);

CREATE TABLE order_detail (
    order_id BIGINT REFERENCES orders(id),
    product_id INT REFERENCES product_basic(id),
    warehouse_id INT NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    price DECIMAL(15,2) NOT NULL CHECK (price >= 0),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (order_id, product_id, warehouse_id)
);

-- BẢNG PHỤC VỤ DỮ LIỆU PHÂN TÁN (REPLICATION & DISTRIBUTED TRANSACTIONS)
CREATE TABLE replication_log (
    id SERIAL PRIMARY KEY,
    entity_id BIGINT NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    action VARCHAR(20) NOT NULL,      -- INSERT, UPDATE, DELETE
    target_site VARCHAR(20) NOT NULL, -- 'DN' hoặc 'HCM'
    status VARCHAR(20) DEFAULT 'PENDING',
    retry_count INT DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE transaction_log (
    transaction_id VARCHAR(100) PRIMARY KEY, -- Global Transaction ID (GTID)
    status VARCHAR(20) NOT NULL,             -- PREPARED, COMMITTED, ABORTED
    participants TEXT,                       -- Danh sách các site tham gia
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE transaction_participant_log (
    id SERIAL PRIMARY KEY,
    transaction_id VARCHAR(100) NOT NULL,
    site_code VARCHAR(10) NOT NULL,
    warehouse_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    status VARCHAR(20) NOT NULL,
    message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- DATA REPLICATION: Dữ liệu dùng chung nhân bản ở tất cả các site
INSERT INTO site (id, site_code, site_name) VALUES
    (1, 'HN', 'Chi nhánh Hà Nội'),
    (2, 'DN', 'Chi nhánh Đà Nẵng'),
    (3, 'HCM', 'Chi nhánh TP.HCM');

INSERT INTO category (id, name) VALUES
    (1, 'Máy tính xách tay'),
    (2, 'Điện thoại thông minh');

INSERT INTO product_basic (id, name, price, category_id) VALUES
    (1, 'MacBook M3', 3000, 1),
    (2, 'iPhone 15 Pro', 1200, 2);

-- Dữ liệu chi tiết (Vertical Fragmentation - Thường đặt tại Master Site)
INSERT INTO product_detail (product_id, description) VALUES
    (1, 'MacBook Air M3 13 inch với hiệu năng mạnh từ chip Apple silicon'),
    (2, 'iPhone 15 Pro vỏ Titanium siêu bền và nhẹ');

-- WAREHOUSE METADATA REPLICATION: danh muc kho duoc nhan ban o tat ca site
INSERT INTO warehouse (id, code, name, location, region, site_id) VALUES
    (1, 'WH-HN-01', 'Kho Hoàn Kiếm', 'Hà Nội', 'North', 1),
    (2, 'WH-DN-01', 'Kho Hải Châu', 'Đà Nẵng', 'Central', 2),
    (3, 'WH-HCM-01', 'Kho Quận 1', 'TP.HCM', 'South', 3);

-- Khởi tạo tồn kho cho các kho tại HN
INSERT INTO inventory (warehouse_id, product_id, quantity) VALUES
    (1, 1, 50),
    (1, 2, 100);

-- CUSTOMER IDENTITY REPLICATION: nhân bản định danh khách hàng ở tất cả các site.
-- Lý do: customer_identity nhỏ, đọc nhiều để xác định main_site, nên nhân bản giúp giảm
-- chi phí truy vấn định tuyến và tăng khả năng sẵn sàng theo mô hình phân bổ dữ liệu.
INSERT INTO customer_identity (id, email, password, main_site_id) VALUES
     (1, 'ana@gmail.com', '123456', 1),
     (2, 'bt@gmail.com', '123456', 2),
     (3, 'cle@gmail.com', '123456', 3);

-- CUSTOMER PROFILE FRAGMENTATION: HN chỉ lưu hồ sơ chi tiết của khách có main_site = HN.
-- Fragment: CustomerProfile_HN = customer_profile ⋈ customer_identity WHERE main_site_id = 1.
INSERT INTO customer_profile (id, name, phone, address) VALUES
     (1, 'Nguyễn Văn A', '0912345678', '123 Phố Huế, Hai Bà Trưng, Hà Nội');

-- Q5 DEMO: order 1001 is coordinated at HN and contains all detail lines here.
-- The lines reference warehouses from HN and DN, so local pushdown can detect multi-site export.
-- Status COMPLETED để dữ liệu này cũng xuất hiện trong report doanh thu/top-selling.
INSERT INTO orders (id, customer_id, status, site_id) VALUES
     (1001, 1, 'COMPLETED', 1);

INSERT INTO order_detail (order_id, product_id, warehouse_id, quantity, price) VALUES
     (1001, 1, 1, 1, 3000.00),
     (1001, 1, 2, 1, 3000.00),
     (1001, 2, 1, 1, 1200.00);

-- DEMO DATASET: electronics store, 6 warehouses total, 50 products, 30 customers.
INSERT INTO category (id, name) VALUES
    (3, 'Máy tính bảng'),
    (4, 'Màn hình'),
    (5, 'Bàn phím'),
    (6, 'Chuột'),
    (7, 'Tai nghe'),
    (8, 'Sạc'),
    (9, 'Đồng hồ thông minh'),
    (10, 'Thiết bị lưu trữ');

INSERT INTO product_basic (id, name, price, category_id) VALUES
    (3, 'Lenovo ThinkPad X1 Carbon', 38990000, 1),
    (4, 'ASUS ROG Zephyrus G14', 42990000, 1),
    (5, 'HP Spectre x360', 34990000, 1),
    (6, 'Samsung Galaxy S24 Ultra', 31990000, 2),
    (7, 'Xiaomi 14', 19990000, 2),
    (8, 'OPPO Find X7', 18990000, 2),
    (9, 'Google Pixel 8 Pro', 23990000, 2),
    (10, 'iPhone 15', 22990000, 2),
    (11, 'iPad Air M2', 16990000, 3),
    (12, 'Samsung Galaxy Tab S9', 19990000, 3),
    (13, 'Xiaomi Pad 6', 8990000, 3),
    (14, 'Lenovo Tab P12', 9990000, 3),
    (15, 'Microsoft Surface Go 4', 14990000, 3),
    (16, 'Dell UltraSharp U2723QE', 13990000, 4),
    (17, 'LG UltraGear 27GP850', 8990000, 4),
    (18, 'Samsung Smart Monitor M8', 12990000, 4),
    (19, 'ASUS ProArt PA278CV', 9490000, 4),
    (20, 'AOC 24G2SP', 4490000, 4),
    (21, 'Keychron K2 Pro', 2490000, 5),
    (22, 'Logitech MX Keys S', 2690000, 5),
    (23, 'Akko 5075B Plus', 1990000, 5),
    (24, 'Razer BlackWidow V4', 3990000, 5),
    (25, 'Corsair K70 RGB', 3590000, 5),
    (26, 'Logitech MX Master 3S', 2490000, 6),
    (27, 'Razer DeathAdder V3', 1690000, 6),
    (28, 'SteelSeries Rival 5', 1490000, 6),
    (29, 'Apple Magic Mouse', 2190000, 6),
    (30, 'Logitech G Pro X Superlight', 3290000, 6),
    (31, 'Sony WH-1000XM5', 8490000, 7),
    (32, 'Apple AirPods Pro 2', 5990000, 7),
    (33, 'Samsung Galaxy Buds2 Pro', 3990000, 7),
    (34, 'JBL Live 770NC', 3490000, 7),
    (35, 'Anker Soundcore Liberty 4', 2490000, 7),
    (36, 'Apple 35W Dual USB-C Charger', 1490000, 8),
    (37, 'Anker Nano II 65W', 1190000, 8),
    (38, 'Baseus GaN 100W', 1590000, 8),
    (39, 'Samsung 45W Super Fast Charger', 990000, 8),
    (40, 'Ugreen Nexode 140W', 2490000, 8),
    (41, 'Apple Watch Series 9', 10990000, 9),
    (42, 'Samsung Galaxy Watch 6', 6990000, 9),
    (43, 'Garmin Venu 3', 10990000, 9),
    (44, 'Xiaomi Watch S3', 3990000, 9),
    (45, 'Amazfit GTR Mini', 2990000, 9),
    (46, 'Samsung 990 Pro 1TB', 3290000, 10),
    (47, 'WD Black SN850X 1TB', 2990000, 10),
    (48, 'SanDisk Extreme Portable SSD 1TB', 3790000, 10),
    (49, 'Seagate One Touch 2TB', 2490000, 10),
    (50, 'Kingston DataTraveler Max 256GB', 990000, 10);

UPDATE product_basic SET name = 'MacBook Air M3 13 inch', price = 29990000 WHERE id = 1;
UPDATE product_basic SET name = 'iPhone 15 Pro', price = 28990000 WHERE id = 2;

INSERT INTO product_detail (product_id, description)
SELECT id, 'Thông tin chi tiết demo cho sản phẩm ' || name || '. Mảnh dọc product_detail được lưu tại site master HN.'
FROM product_basic
WHERE id >= 3;

UPDATE product_detail
SET description = 'Thông tin chi tiết demo cho sản phẩm MacBook Air M3 13 inch. Mảnh dọc product_detail được lưu tại site master HN.'
WHERE product_id = 1;

UPDATE product_detail
SET description = 'Thông tin chi tiết demo cho sản phẩm iPhone 15 Pro. Mảnh dọc product_detail được lưu tại site master HN.'
WHERE product_id = 2;

INSERT INTO warehouse (id, code, name, location, region, site_id) VALUES
    (4, 'WH-HN-02', 'Kho Cầu Giấy', 'Hà Nội', 'North', 1),
    (5, 'WH-DN-02', 'Kho Liên Chiểu', 'Đà Nẵng', 'Central', 2),
    (6, 'WH-HCM-02', 'Kho Thủ Đức', 'TP.HCM', 'South', 3);

UPDATE inventory SET quantity = 25 WHERE warehouse_id = 1 AND product_id = 1;
UPDATE inventory SET quantity = 80 WHERE warehouse_id = 1 AND product_id = 2;

INSERT INTO inventory (warehouse_id, product_id, quantity) VALUES
    (4, 1, 15),
    (4, 2, 30),
    (1, 10, 25),
    (4, 10, 15),
    (1, 45, 2),
    (4, 45, 2);

INSERT INTO inventory (warehouse_id, product_id, quantity)
SELECT w.warehouse_id, p.id,
       ((p.id * 7 + w.warehouse_id * 11) % 35) + 10
FROM product_basic p
CROSS JOIN (VALUES (1), (4)) AS w(warehouse_id)
WHERE p.id NOT IN (1, 2, 10, 45);

INSERT INTO customer_identity (id, email, password, main_site_id) VALUES
     (4, 'hn04@example.com', '123456', 1),
     (5, 'hn05@example.com', '123456', 1),
     (6, 'hn06@example.com', '123456', 1),
     (7, 'hn07@example.com', '123456', 1),
     (8, 'hn08@example.com', '123456', 1),
     (9, 'hn09@example.com', '123456', 1),
     (10, 'hn10@example.com', '123456', 1),
     (11, 'hn11@example.com', '123456', 1),
     (12, 'hn12@example.com', '123456', 1),
     (13, 'dn13@example.com', '123456', 2),
     (14, 'dn14@example.com', '123456', 2),
     (15, 'dn15@example.com', '123456', 2),
     (16, 'dn16@example.com', '123456', 2),
     (17, 'dn17@example.com', '123456', 2),
     (18, 'dn18@example.com', '123456', 2),
     (19, 'dn19@example.com', '123456', 2),
     (20, 'dn20@example.com', '123456', 2),
     (21, 'dn21@example.com', '123456', 2),
     (22, 'hcm22@example.com', '123456', 3),
     (23, 'hcm23@example.com', '123456', 3),
     (24, 'hcm24@example.com', '123456', 3),
     (25, 'hcm25@example.com', '123456', 3),
     (26, 'hcm26@example.com', '123456', 3),
     (27, 'hcm27@example.com', '123456', 3),
     (28, 'hcm28@example.com', '123456', 3),
     (29, 'hcm29@example.com', '123456', 3),
     (30, 'hcm30@example.com', '123456', 3);

INSERT INTO customer_profile (id, name, phone, address)
SELECT id,
       'Khách hàng HN ' || id,
       '09123' || LPAD(id::text, 5, '0'),
       'Địa chỉ demo tại Hà Nội ' || id
FROM customer_identity
WHERE main_site_id = 1 AND id <> 1;

INSERT INTO orders (id, customer_id, status, site_id, order_date)
SELECT 1001 + g,
       (ARRAY[1,4,5,6,7,8,9,10,11,12])[((g - 1) % 10) + 1],
       CASE WHEN g <= 30 THEN 'COMPLETED' ELSE 'PENDING' END,
       1,
       CURRENT_TIMESTAMP - (g || ' days')::interval
FROM generate_series(1, 35) AS g;

INSERT INTO order_detail (order_id, product_id, warehouse_id, quantity, price)
SELECT 1001 + g,
       ((g - 1) % 50) + 1,
       CASE WHEN g % 2 = 0 THEN 4 ELSE 1 END,
       (g % 3) + 1,
       p.price
FROM generate_series(1, 35) AS g
JOIN product_basic p ON p.id = ((g - 1) % 50) + 1;

INSERT INTO order_detail (order_id, product_id, warehouse_id, quantity, price)
SELECT 1001 + g,
       ((g + 4) % 50) + 1,
       CASE WHEN g % 10 = 0 THEN 2 ELSE 3 END,
       1,
       p.price
FROM generate_series(5, 35, 5) AS g
JOIN product_basic p ON p.id = ((g + 4) % 50) + 1;

UPDATE order_detail od
SET price = pb.price
FROM product_basic pb
WHERE od.product_id = pb.id;

-- CẬP NHẬT LẠI SEQUENCE CHO CÁC BẢNG CÓ KHÓA CHÍNH TỰ TĂNG (SERIAL)
SELECT setval('category_id_seq', (SELECT MAX(id) FROM category));
SELECT setval('product_basic_id_seq', (SELECT MAX(id) FROM product_basic));
SELECT setval('site_id_seq', (SELECT MAX(id) FROM site));

SELECT setval('warehouse_id_seq', (SELECT MAX(id) FROM warehouse));
-- Không cần setval cho customer_identity vì dùng BIGINT (Snowflake/Manual ID)
SELECT setval('replication_log_id_seq', COALESCE((SELECT MAX(id) FROM replication_log), 1));
SELECT setval('transaction_participant_log_id_seq', COALESCE((SELECT MAX(id) FROM transaction_participant_log), 1));

-- INDICES
CREATE INDEX idx_inventory_product ON inventory(product_id);
CREATE INDEX idx_inventory_warehouse ON inventory(warehouse_id);
CREATE INDEX idx_order_detail_product ON order_detail(product_id);
CREATE INDEX idx_order_detail_warehouse ON order_detail(warehouse_id);
CREATE INDEX idx_orders_date ON orders(order_date);
CREATE INDEX idx_orders_site ON orders(site_id);
CREATE INDEX idx_customer_identity_main_site ON customer_identity(main_site_id);
CREATE INDEX idx_transaction_participant_log_tx ON transaction_participant_log(transaction_id);
