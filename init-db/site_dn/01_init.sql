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
    PRIMARY KEY (warehouse_id, product_id)
);

CREATE TABLE customer_identity (
    id BIGINT PRIMARY KEY,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL,
    main_site_id INT REFERENCES site(id) NOT NULL
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
    warehouse_id INT REFERENCES warehouse(id), -- kho chính xử lý
    site_id INT REFERENCES site(id) -- site tạo đơn
);

CREATE TABLE order_detail (
    order_id BIGINT REFERENCES orders(id),
    product_id INT REFERENCES product_basic(id),
    warehouse_id INT REFERENCES warehouse(id),
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

-- FRAGMENTATION: Phân mảnh ngang (Primary Horizontal) cho Warehouse miền Trung
INSERT INTO warehouse (id, code, name, location, region, site_id) VALUES
    (2, 'WH-DN-01', 'Kho Hải Châu', 'Đà Nẵng', 'Central', 2);

-- Khởi tạo tồn kho cho các kho tại DN
INSERT INTO inventory (warehouse_id, product_id, quantity) VALUES
    (2, 1, 20),
    (2, 2, 40);

-- Dữ liệu khách hàng cục bộ tại DN
INSERT INTO customer_identity (id, email, password, main_site_id) VALUES
    (2, 'bt@gmail.com', '123456', 2);

INSERT INTO customer_profile (id, name, phone, address) VALUES
    (2, 'Tran Thi B', '0987654321', '456 Le Duan, Hai Chau, Da Nang');

-- CẬP NHẬT LẠI SEQUENCE CHO CÁC BẢNG CÓ KHÓA CHÍNH TỰ TĂNG (SERIAL)
SELECT setval('category_id_seq', (SELECT MAX(id) FROM category));
SELECT setval('product_basic_id_seq', (SELECT MAX(id) FROM product_basic));
SELECT setval('site_id_seq', (SELECT MAX(id) FROM site));
SELECT setval('warehouse_id_seq', (SELECT MAX(id) FROM warehouse));
-- Không cần setval cho customer_identity vì dùng BIGINT (Snowflake/Manual ID)
SELECT setval('replication_log_id_seq', COALESCE((SELECT MAX(id) FROM replication_log), 1));

-- INDICES
CREATE INDEX idx_inventory_product ON inventory(product_id);
CREATE INDEX idx_inventory_warehouse ON inventory(warehouse_id);
CREATE INDEX idx_order_detail_product ON order_detail(product_id);
CREATE INDEX idx_order_detail_warehouse ON order_detail(warehouse_id);
CREATE INDEX idx_orders_date ON orders(order_date);
CREATE INDEX idx_orders_site ON orders(site_id);