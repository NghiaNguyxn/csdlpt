CREATE TABLE category (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE product_basic (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(15,2) NOT NULL CHECK (price >= 0),
    category_id INT REFERENCES category(id)
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

CREATE TABLE customer (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE,
    phone VARCHAR(20),
    address TEXT,
    main_site_id INT REFERENCES site(id)
);

CREATE TABLE orders (
    id BIGINT PRIMARY KEY,
    customer_id INT REFERENCES customer(id),
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'COMPLETED', 'CANCELLED')),
    warehouse_id INT REFERENCES warehouse(id), -- kho chính xử lý
    site_id INT REFERENCES site(id) -- site tạo đơn
);

CREATE TABLE order_detail (
    order_id INT REFERENCES orders(id),
    product_id INT REFERENCES product_basic(id),
    warehouse_id INT REFERENCES warehouse(id),
    quantity INT NOT NULL CHECK (quantity > 0),
    price DECIMAL(15,2) NOT NULL CHECK (price >= 0),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (order_id, product_id, warehouse_id)
);

CREATE INDEX idx_inventory_product ON inventory(product_id);
CREATE INDEX idx_inventory_warehouse ON inventory(warehouse_id);

CREATE INDEX idx_order_detail_product ON order_detail(product_id);
CREATE INDEX idx_order_detail_warehouse ON order_detail(warehouse_id);

CREATE INDEX idx_orders_date ON orders(order_date);
CREATE INDEX idx_orders_site ON orders(site_id);

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
INSERT INTO customer (id, name, email, main_site_id) VALUES
    (2, 'Tran Thi B', 'bt@gmail.com', 2);