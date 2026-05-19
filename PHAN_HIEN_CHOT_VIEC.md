# Chốt phần việc của Hiển

Bản này được merge trên nền code/CSDL của Nghĩa và bổ sung các phần việc của Hiển.

## 1. CSDL đã chốt

- `warehouse` được nhân bản ở cả 3 site HN, DN, HCM.
- `inventory` vẫn phân mảnh ngang theo kho/site:
  - HN chỉ có inventory của warehouse_id = 1.
  - DN chỉ có inventory của warehouse_id = 2.
  - HCM chỉ có inventory của warehouse_id = 3.
- Đã thêm dữ liệu mẫu `orders`, `order_detail` để demo Q3, Q4.
- Đã thêm đơn liên kho mẫu ở HCM: order 3002 lấy hàng từ DN và HCM.

## 2. Nhiệm vụ Hiển đã có code tương ứng

### Global schema
- Review được ERD/schema dựa trên các bảng: category, product_basic, product_detail, warehouse, inventory, customer_identity, customer_profile, orders, order_detail, replication_log, transaction_log.

### Fragmentation
- Inventory phân mảnh ngang theo warehouse/site.
- Orders phân mảnh theo site tạo đơn/kho chính xử lý.
- Order_detail là phân mảnh dẫn xuất theo orders, đồng thời có `warehouse_id` để biểu diễn kho xuất hàng.

### Replication
- Category được nhân bản tương tự product_basic.
- API:
  - GET /api/categories
  - POST /api/categories
  - PUT /api/categories/{id}
  - DELETE /api/categories/{id}
- ReplicationJob xử lý thêm `entity_type = CATEGORY`.

### Distributed query
- Q3: GET /api/reports/revenue/monthly?year=2026
- Q4: GET /api/reports/top-selling?limit=5

### Distributed transaction
- API: POST /api/orders/multi
- Mô phỏng 2PC bằng transaction_log: PREPARED, COMMITTED, ABORTED.
- API xem log:
  - GET /api/transactions
  - GET /api/transactions/{transactionId}

### Concurrency
- Inventory giảm tồn kho bằng atomic conditional update, tránh âm tồn kho.
- API test race condition:
  - POST /api/test/race-condition

### Demo
- GET /api/reports/revenue/monthly?year=2026
- GET /api/reports/top-selling?limit=5
- POST /api/orders/multi
- GET /api/transactions
- POST /api/test/race-condition

## 3. Thứ tự chạy test

1. Chạy lại database:

```bash
docker compose down -v
docker compose up -d
```

2. Chạy Spring Boot.

3. Test dữ liệu nền:

```http
GET /api/warehouses
GET /api/categories
```

4. Test Q3, Q4:

```http
GET /api/reports/revenue/monthly?year=2026
GET /api/reports/top-selling?limit=5
```

5. Test đơn liên kho:

```http
POST /api/orders/multi
```

Body:

```json
{
  "orderId": 4001,
  "customerId": 3,
  "mainSite": "HCM",
  "mainWarehouseId": 3,
  "items": [
    {
      "productId": 1,
      "allocations": [
        { "siteCode": "DN", "warehouseId": 2, "quantity": 3 },
        { "siteCode": "HCM", "warehouseId": 3, "quantity": 5 }
      ]
    }
  ]
}
```

6. Xem log 2PC:

```http
GET /api/transactions
```

7. Test race condition:

```http
POST /api/test/race-condition
```

Body:

```json
{
  "siteCode": "HN",
  "warehouseId": 1,
  "productId": 1,
  "initialQuantity": 1,
  "quantityPerOrder": 1,
  "threadCount": 2
}
```

Kết quả mong muốn: `successCount = 1`, `failedCount = 1`, `finalQuantity = 0`.
