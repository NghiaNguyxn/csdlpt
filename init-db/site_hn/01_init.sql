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
    (1, 'Laptop'),
    (2, 'Smartphone');

INSERT INTO product_basic (id, name, price, category_id) VALUES
    (1, 'Macbook M3', 3000, 1),
    (2, 'iPhone 15 Pro', 1200, 2);

-- Dữ liệu chi tiết (Vertical Fragmentation - Thường đặt tại Master Site)
INSERT INTO product_detail (product_id, description) VALUES
    (1, 'Macbook Pro M3 với hiệu năng cực mạnh từ chip 3nm Apple silicon'),
    (2, 'iPhone 15 Pro vỏ Titanium siêu bền và nhẹ');

-- FRAGMENTATION: Phân mảnh ngang (Primary Horizontal) cho Warehouse miền Bắc
INSERT INTO warehouse (id, code, name, location, region, site_id) VALUES
    (1, 'WH-HN-01', 'Kho Hoàn Kiếm', 'Hà Nội', 'North', 1);

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
     (1, 'Nguyen Van A', '0912345678', '123 Pho Hue, Hai Ba Trung, Ha Noi');

-- Q5 DEMO: đơn 1001 được xuất từ nhiều kho ở nhiều site.
-- Tại HN giữ phần xuất từ WH-HN-01; DN giữ phần còn lại từ WH-DN-01.
INSERT INTO orders (id, customer_id, status, site_id) VALUES
     (1001, 1, 'PENDING', 1);

INSERT INTO order_detail (order_id, product_id, warehouse_id, quantity, price) VALUES
     (1001, 1, 1, 1, 3000.00),
     (1001, 2, 1, 1, 1200.00);

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
