# Hướng Dẫn Setup Bảng Reviews

## Vấn đề
Nếu bạn gặp lỗi 500 khi load reviews hoặc 404 khi gọi API reviewable-bookings, có thể bảng `reviews` chưa được tạo trong database.

## Giải pháp

### Bước 1: Chạy Migration SQL

1. Mở **SQL Server Management Studio** (SSMS)
2. Kết nối đến database: `hotel_booking_system`
3. Mở file: `be_hotel/src/main/resources/db/migration/V6__create_reviews_table.sql`
4. Copy toàn bộ nội dung và chạy (Execute)

### Bước 2: Verify Bảng Đã Được Tạo

Chạy query sau để kiểm tra:

```sql
-- Kiểm tra bảng reviews
SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'reviews';

-- Kiểm tra các cột
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'reviews'
ORDER BY ORDINAL_POSITION;

-- Kiểm tra foreign keys
SELECT 
    fk.name AS ForeignKey,
    tp.name AS ParentTable,
    cp.name AS ParentColumn,
    tr.name AS ReferencedTable,
    cr.name AS ReferencedColumn
FROM sys.foreign_keys fk
INNER JOIN sys.foreign_key_columns fkc ON fk.object_id = fkc.constraint_object_id
INNER JOIN sys.tables tp ON fkc.parent_object_id = tp.object_id
INNER JOIN sys.columns cp ON fkc.parent_object_id = cp.object_id AND fkc.parent_column_id = cp.column_id
INNER JOIN sys.tables tr ON fkc.referenced_object_id = tr.object_id
INNER JOIN sys.columns cr ON fkc.referenced_object_id = cr.object_id AND fkc.referenced_column_id = cr.column_id
WHERE tp.name = 'reviews';
```

### Bước 3: Restart Backend Server

Sau khi tạo bảng, restart backend server để đảm bảo các thay đổi được áp dụng.

## Cấu Trúc Bảng Reviews

```sql
reviews
├── review_id (PK, INT, IDENTITY)
├── booking_id (FK → bookings.booking_id, NOT NULL)
├── rating (INT, NOT NULL, 1-5)
├── comment (NVARCHAR(MAX), NULL)
└── created_at (DATETIME, NOT NULL, DEFAULT GETDATE())

Constraints:
├── FK_reviews_booking (booking_id → bookings.booking_id)
└── CK_reviews_rating (rating >= 1 AND rating <= 5)

Indexes:
├── IX_reviews_booking_id (booking_id) INCLUDE (rating, created_at)
└── IX_reviews_created_at (created_at DESC)
```

## Test API

Sau khi setup xong, test các API sau:

1. **Lấy reviews của phòng:**
   ```
   GET http://localhost:8080/api/reviews/room/5
   ```

2. **Lấy rating của phòng:**
   ```
   GET http://localhost:8080/api/reviews/room/5/rating
   ```

3. **Lấy reviewable bookings (cần token):**
   ```
   GET http://localhost:8080/api/reviews/room/5/reviewable-bookings
   Headers: X-Auth-Token: <token>
   ```

## Troubleshooting

### Lỗi: "Invalid object name 'reviews'"
**Giải pháp:** Bảng reviews chưa được tạo. Chạy lại migration SQL.

### Lỗi: "Foreign key constraint violation"
**Giải pháp:** Đảm bảo bảng `bookings` đã tồn tại và có dữ liệu hợp lệ.

### Lỗi: 404 khi gọi reviewable-bookings
**Giải pháp:** 
- Kiểm tra backend server đã restart chưa
- Kiểm tra endpoint có đúng không: `/api/reviews/room/{roomId}/reviewable-bookings`
- Đảm bảo đã gửi header `X-Auth-Token`

### Lỗi: 500 khi lấy reviews
**Giải pháp:**
- Kiểm tra bảng reviews đã được tạo chưa
- Kiểm tra log backend để xem lỗi cụ thể
- Đảm bảo database connection đúng

