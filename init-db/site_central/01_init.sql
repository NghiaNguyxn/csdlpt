CREATE TABLE central_inventory (
    warehouse_id INT NOT NULL,
    product_id INT NOT NULL,
    site_code VARCHAR(10) NOT NULL,
    quantity INT NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    reserved_quantity INT NOT NULL DEFAULT 0 CHECK (reserved_quantity >= 0),
    PRIMARY KEY (site_code, warehouse_id, product_id)
);

CREATE INDEX idx_central_inventory_product ON central_inventory(product_id);
CREATE INDEX idx_central_inventory_site ON central_inventory(site_code);

-- Database tập trung đối chứng cho benchmark Q6.
-- Dữ liệu inventory ban đầu tương ứng với 3 site phân tán HN, DN, HCM.
INSERT INTO central_inventory (warehouse_id, product_id, site_code, quantity, reserved_quantity) VALUES
    (1, 1, 'HN', 50, 0),
    (1, 2, 'HN', 100, 0),
    (2, 1, 'DN', 20, 0),
    (2, 2, 'DN', 40, 0),
    (3, 1, 'HCM', 100, 0),
    (3, 2, 'HCM', 200, 0);
