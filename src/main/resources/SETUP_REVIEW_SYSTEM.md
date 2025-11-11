# 🚀 Hướng Dẫn Setup Hệ Thống Đánh Giá (Review System)

## 📋 Tổng Quan

Hệ thống đánh giá cho phép khách hàng đánh giá phòng sau khi đặt phòng thành công. Hệ thống bao gồm:
- ✅ Đánh giá tổng thể (1-5 sao)
- ✅ Đánh giá giá cả (1-5 sao, tùy chọn)
- ✅ Comment đánh giá
- ✅ Hiển thị rating trung bình
- ✅ Histogram phân bố rating

## 🗄️ Database Setup

### Bước 1: Chạy Migration SQL

Chạy file migration để thêm cột `price_rating` và các constraints:

```sql
-- File: be_hotel/src/main/resources/db/migration/V6__add_price_rating_to_reviews.sql
-- Chạy script này trong SQL Server Management Studio
```

Hoặc nếu database chưa có bảng `reviews`, chạy script tạo bảng:

```sql
-- Tạo bảng reviews (nếu chưa có)
CREATE TABLE [dbo].[reviews](
    [review_id] [int] IDENTITY(1,1) NOT NULL PRIMARY KEY,
    [booking_id] [int] NOT NULL,
    [rating] [int] NOT NULL,
    [price_rating] [int] NULL,
    [comment] [nvarchar](max) NULL,
    [created_at] [datetime] NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_reviews_booking FOREIGN KEY (booking_id) REFERENCES bookings(booking_id),
    CONSTRAINT CK_reviews_rating CHECK (rating >= 1 AND rating <= 5),
    CONSTRAINT CK_reviews_price_rating CHECK (price_rating IS NULL OR (price_rating >= 1 AND price_rating <= 5))
);

-- Tạo index để tối ưu query
CREATE INDEX IX_reviews_booking_id ON reviews(booking_id) INCLUDE (rating, price_rating, created_at);
```

### Bước 2: Verify Database

Kiểm tra xem bảng và các constraints đã được tạo:

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

## 🔧 Backend Setup

### Đã có sẵn:

1. **ReviewEntity** (`be_hotel/src/main/java/com/luxestay/hotel/model/entity/ReviewEntity.java`)
   - Entity mapping với bảng `reviews`
   - Có đầy đủ fields: rating, priceRating, comment, createdAt

2. **ReviewRepository** (`be_hotel/src/main/java/com/luxestay/hotel/repository/ReviewRepository.java`)
   - Các query methods để lấy reviews, tính rating trung bình
   - Histogram rating distribution

3. **ReviewService** (`be_hotel/src/main/java/com/luxestay/hotel/service/ReviewService.java`)
   - Business logic: validation, permission check, anti-spam
   - Tạo review, lấy reviews theo room, tính rating

4. **ReviewController** (`be_hotel/src/main/java/com/luxestay/hotel/controller/ReviewController.java`)
   - REST API endpoints:
     - `POST /api/reviews` - Tạo đánh giá
     - `GET /api/reviews/room/{roomId}` - Lấy đánh giá theo phòng
     - `GET /api/reviews/room/{roomId}/rating` - Lấy rating trung bình
     - `GET /api/reviews/featured` - Lấy featured reviews

5. **DTOs**:
   - `CreateReviewRequest` - Request body khi tạo review
   - `ReviewDTO` - Response DTO
   - `RoomRatingDTO` - Rating trung bình và histogram

## 🎨 Frontend Setup

### Đã có sẵn:

1. **ReviewForm.jsx** (`fe_hotel/src/components/review/ReviewForm.jsx`)
   - Form để tạo đánh giá
   - Có field đánh giá tổng thể và đánh giá giá cả

2. **ReviewModal.jsx** (`fe_hotel/src/components/review/ReviewModal.jsx`)
   - Modal popup để tạo đánh giá
   - Tương tự ReviewForm nhưng trong modal

3. **ReviewList.jsx** (`fe_hotel/src/components/review/ReviewList.jsx`)
   - Hiển thị danh sách đánh giá
   - Hiển thị rating và price rating

4. **RoomRating.jsx** (`fe_hotel/src/components/review/RoomRating.jsx`)
   - Hiển thị rating trung bình
   - Histogram phân bố rating

### Sử dụng trong RoomDetail:

```jsx
import RoomRating from '../components/review/RoomRating'
import ReviewList from '../components/review/ReviewList'
import ReviewForm from '../components/review/ReviewForm'

// Trong RoomDetail component
<RoomRating roomId={roomId} />
<ReviewList roomId={roomId} />
<ReviewForm bookingId={bookingId} roomId={roomId} onSuccess={handleReviewSuccess} />
```

## 🧪 Testing

### Test API Endpoints:

1. **Tạo đánh giá**:
```bash
POST http://localhost:8080/api/reviews
Headers: X-Auth-Token: <token>
Body: {
    "bookingId": 1,
    "rating": 5,
    "priceRating": 4,
    "comment": "Phòng rất đẹp, giá hợp lý!"
}
```

2. **Lấy đánh giá theo phòng**:
```bash
GET http://localhost:8080/api/reviews/room/1
```

3. **Lấy rating trung bình**:
```bash
GET http://localhost:8080/api/reviews/room/1/rating
```

### Test Frontend:

1. Mở trang chi tiết phòng
2. Cuộn xuống phần "Đánh giá của khách"
3. Xem rating trung bình và histogram
4. Xem danh sách đánh giá
5. Tạo đánh giá mới (nếu có booking)

## ✅ Checklist Setup

- [ ] Database: Bảng `reviews` đã được tạo
- [ ] Database: Cột `price_rating` đã được thêm
- [ ] Database: Foreign key `FK_reviews_booking` đã được tạo
- [ ] Database: Check constraints đã được tạo
- [ ] Database: Index `IX_reviews_booking_id` đã được tạo
- [ ] Backend: ReviewEntity đã được cập nhật
- [ ] Backend: ReviewService đã có validation
- [ ] Backend: ReviewController đã có các endpoints
- [ ] Frontend: ReviewForm đã có field priceRating
- [ ] Frontend: ReviewList đã hiển thị priceRating
- [ ] Frontend: RoomRating đã hiển thị rating trung bình

## 🚨 Troubleshooting

### Lỗi: "Column price_rating does not exist"
**Giải pháp**: Chạy migration script V6__add_price_rating_to_reviews.sql

### Lỗi: "Foreign key constraint violation"
**Giải pháp**: Đảm bảo `booking_id` tồn tại trong bảng `bookings`

### Lỗi: "You can only review your own bookings"
**Giải pháp**: Đảm bảo user đang đăng nhập đúng và booking thuộc về user đó

### Lỗi: "You have already reviewed this booking"
**Giải pháp**: Mỗi booking chỉ được đánh giá 1 lần (anti-spam)

### Lỗi: "Bạn chỉ có thể đánh giá sau khi đặt phòng thành công"
**Giải pháp**: Booking status phải là `confirmed`, `checked_in`, `checked_out`, hoặc `completed`

## 📊 Database Schema

```
reviews
├── review_id (PK, INT, IDENTITY)
├── booking_id (FK → bookings.booking_id, NOT NULL)
├── rating (INT, NOT NULL, 1-5)
├── price_rating (INT, NULL, 1-5)
├── comment (NVARCHAR(MAX), NULL)
└── created_at (DATETIME, NOT NULL, DEFAULT GETDATE())

Constraints:
├── FK_reviews_booking (booking_id → bookings.booking_id)
├── CK_reviews_rating (rating >= 1 AND rating <= 5)
└── CK_reviews_price_rating (price_rating IS NULL OR (price_rating >= 1 AND price_rating <= 5))

Indexes:
└── IX_reviews_booking_id (booking_id) INCLUDE (rating, price_rating, created_at)
```

## 🎉 Kết Luận

Hệ thống đánh giá đã được setup đầy đủ và sẵn sàng sử dụng! Chỉ cần chạy migration script để update database là có thể sử dụng ngay.

Nếu có vấn đề gì, vui lòng kiểm tra:
1. Database connection
2. Migration script đã chạy chưa
3. Backend service đang chạy
4. Frontend API endpoints đúng chưa

