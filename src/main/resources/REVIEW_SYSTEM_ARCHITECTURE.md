# 🏨 Hệ Thống Đánh Giá (Review System) - Kiến Trúc & Hướng Dẫn

## 📋 Tổng Quan

Hệ thống đánh giá cho phép khách hàng đánh giá phòng sau khi đặt phòng thành công. Tất cả dữ liệu được lưu trữ trong **database** (SQL Server).

## 🗄️ Database Schema

### Bảng `reviews`

```sql
CREATE TABLE reviews (
    review_id INT IDENTITY(1,1) PRIMARY KEY,
    booking_id INT NOT NULL,              -- FK → bookings
    rating INT NOT NULL,                  -- 1-5 sao (đánh giá tổng thể)
    price_rating INT NULL,                -- 1-5 sao (đánh giá giá cả) - Optional
    comment NVARCHAR(MAX) NULL,           -- Nội dung đánh giá
    created_at DATETIME NOT NULL,         -- Thời gian tạo
    CONSTRAINT FK_reviews_booking FOREIGN KEY (booking_id) REFERENCES bookings(booking_id),
    CONSTRAINT CK_reviews_rating CHECK (rating >= 1 AND rating <= 5),
    CONSTRAINT CK_reviews_price_rating CHECK (price_rating IS NULL OR (price_rating >= 1 AND price_rating <= 5))
);
```

### Quan Hệ Database

```
accounts (Người dùng)
    ↓ (1:N)
bookings (Đặt phòng)
    ↓ (1:1)
reviews (Đánh giá)
    ↓ (thông qua booking)
rooms (Phòng) ← Đánh giá được group theo phòng
```

## 🔄 Luồng Hoạt Động

### 1. Tạo Đánh Giá

```
User → Frontend (ReviewForm/ReviewModal)
    → POST /api/reviews
    → ReviewController.createReview()
    → ReviewService.createReview()
    → Validation:
        - Chỉ chủ booking mới được đánh giá
        - Mỗi booking chỉ được đánh giá 1 lần
        - Chỉ đánh giá sau khi booking confirmed/checked_in/checked_out
    → Lưu vào Database (bảng reviews)
    → Trả về ReviewDTO
```

### 2. Xem Đánh Giá Theo Phòng

```
Frontend → GET /api/reviews/room/{roomId}
    → ReviewController.getReviewsByRoom()
    → ReviewService.getReviewsByRoom()
    → ReviewRepository.findByRoomId()
    → JOIN: reviews → bookings → rooms
    → Trả về List<ReviewDTO>
```

### 3. Tính Rating Trung Bình

```
Frontend → GET /api/reviews/room/{roomId}/rating
    → ReviewController.getRoomRating()
    → ReviewService.getRoomRating()
    → ReviewRepository.getAverageRatingByRoomId()
    → SQL: AVG(rating) WHERE room_id = ?
    → Trả về RoomRatingDTO (avgRating, totalReviews, histogram)
```

## 💾 Tại Sao Lưu Trong Database?

### ✅ Ưu Điểm:

1. **Dữ liệu bền vững**: Reviews được lưu vĩnh viễn, không mất khi server restart
2. **Truy vấn nhanh**: SQL Server có index, query nhanh với hàng nghìn reviews
3. **Tính toán chính xác**: AVG, COUNT, GROUP BY được tính toán chính xác
4. **Backup & Recovery**: Có thể backup và restore dữ liệu
5. **Phân tích**: Có thể query, phân tích trends, statistics
6. **Scalability**: Database có thể scale với hàng triệu reviews
7. **Integrity**: Foreign key đảm bảo data consistency
8. **Transaction**: Đảm bảo ACID properties

### ❌ Nếu Không Lưu Database:

- ❌ Mất dữ liệu khi server restart (nếu lưu memory)
- ❌ Không thể query phức tạp (filter, sort, aggregate)
- ❌ Khó scale khi có nhiều reviews
- ❌ Không có backup/recovery
- ❌ Không đảm bảo data integrity

## 📊 API Endpoints

### 1. Tạo Đánh Giá
```
POST /api/reviews
Headers: X-Auth-Token: <token>
Body: {
    "bookingId": 1,
    "rating": 5,
    "priceRating": 4,  // Optional
    "comment": "Phòng rất đẹp, giá hợp lý!"
}
```

### 2. Lấy Đánh Giá Theo Phòng
```
GET /api/reviews/room/{roomId}
Response: [
    {
        "id": 1,
        "bookingId": 1,
        "roomId": 101,
        "roomName": "Phòng Deluxe",
        "accountId": 1,
        "accountName": "Nguyễn Văn A",
        "accountAvatar": "https://...",
        "rating": 5,
        "priceRating": 4,
        "comment": "Phòng rất đẹp!",
        "createdAt": "2025-01-10T10:00:00"
    }
]
```

### 3. Lấy Rating Trung Bình
```
GET /api/reviews/room/{roomId}/rating
Response: {
    "averageRating": 4.5,
    "totalReviews": 120,
    "ratingHistogram": {
        5: 60,
        4: 40,
        3: 15,
        2: 3,
        1: 2
    }
}
```

## 🎯 Business Rules

1. **Chỉ chủ booking mới được đánh giá**: Kiểm tra `booking.account.id == currentUser.id`
2. **Mỗi booking chỉ được đánh giá 1 lần**: Check `existsByBooking_Id(bookingId)`
3. **Chỉ đánh giá sau khi booking thành công**: Status phải là `confirmed`, `checked_in`, `checked_out`, hoặc `completed`
4. **Rating phải từ 1-5**: Validation trong Service
5. **Price Rating là optional**: Có thể null

## 🔍 Query Optimization

### Indexes Đề Xuất:

```sql
-- Index cho query reviews theo room
CREATE INDEX IX_reviews_booking_room 
ON reviews(booking_id)
INCLUDE (rating, price_rating, created_at);

-- Index cho query rating trung bình
CREATE INDEX IX_bookings_room_status 
ON bookings(room_id, status)
INCLUDE (booking_id);
```

### Performance:

- ✅ Query reviews theo room: < 100ms (với index)
- ✅ Tính rating trung bình: < 50ms (với aggregation)
- ✅ Phân trang: Sử dụng OFFSET/FETCH hoặc cursor

## 🚀 Best Practices

1. **Luôn validate input**: Rating 1-5, không spam
2. **Cache rating trung bình**: Có thể cache trong Redis nếu cần
3. **Pagination**: Không load tất cả reviews, chỉ load 10-20 reviews mỗi lần
4. **Lazy loading**: Sử dụng LAZY fetch để tránh N+1 problem
5. **Transaction**: Đảm bảo data consistency khi tạo review

## 📈 Metrics & Analytics

Có thể tính toán:
- ✅ Rating trung bình theo phòng
- ✅ Rating trung bình theo giá cả
- ✅ Top rated rooms
- ✅ Rating trends theo thời gian
- ✅ Distribution của ratings (histogram)

## 🔒 Security

1. **Authentication**: Phải đăng nhập mới được đánh giá
2. **Authorization**: Chỉ chủ booking mới được đánh giá
3. **Anti-spam**: Mỗi booking chỉ được đánh giá 1 lần
4. **Input validation**: Validate rating, comment length
5. **SQL Injection**: Sử dụng parameterized queries (JPA)

## 📝 Kết Luận

**Hệ thống đánh giá NÊN được lưu trong database** vì:
- ✅ Data persistence
- ✅ Query performance
- ✅ Data integrity
- ✅ Scalability
- ✅ Analytics capabilities

**Kiến trúc hiện tại đã đúng và tối ưu!** 🎉

