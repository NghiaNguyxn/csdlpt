# csdlpt

## API kho hàng đã bổ sung

Các endpoint kho hàng:

```text
GET    /api/warehouses
POST   /api/warehouses
PUT    /api/warehouses/{id}
DELETE /api/warehouses/{id}
```

Ví dụ thêm kho:

```http
POST http://localhost:8080/api/warehouses
Content-Type: application/json
```

```json
{
  "code": "WH-HN-02",
  "name": "Kho Cầu Giấy",
  "location": "Hà Nội",
  "region": "North",
  "siteCode": "HN"
}
```

Lưu ý: `DELETE /api/warehouses/{id}` sẽ chặn xóa nếu kho vẫn còn dữ liệu trong bảng `inventory` ở bất kỳ site nào.
