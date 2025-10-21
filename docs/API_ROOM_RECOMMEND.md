# 🎯 Room Recommendation API Documentation

## Overview

Chức năng gợi ý phòng (Room Recommendation) sử dụng các thuật toán baseline để đề xuất phòng phù hợp nhất cho người dùng dựa trên nhiều yếu tố khác nhau.

---

## 📡 API Endpoint

```
GET /api/rooms/recommend
```

### Base URL

```
http://localhost:8080/api/rooms/recommend
```

---

## 🔧 Request Parameters

| Parameter   | Type    | Required | Default | Description                                     |
| ----------- | ------- | -------- | ------- | ----------------------------------------------- |
| `accountId` | Long    | No       | null    | User ID for personalized recommendations        |
| `type`      | String  | No       | "auto"  | Recommendation algorithm type                   |
| `limit`     | Integer | No       | 5       | Number of rooms to return (max recommended: 10) |

### Recommendation Types

| Type           | Description                                                          | Use Case                      |
| -------------- | -------------------------------------------------------------------- | ----------------------------- |
| `auto`         | **Automatic** - Tries personalized → popular → top_rated → available | Default, best for general use |
| `personalized` | Based on user's booking history (bed layout preference)              | For logged-in users           |
| `popular`      | Most frequently booked rooms                                         | Homepage highlights           |
| `top_rated`    | Highest rated rooms (requires reviews data)                          | Quality showcase              |

---

## 📥 Response Format

### Success (200 OK)

Returns an array of `Room` objects:

```json
[
  {
    "id": 21,
    "roomNumber": "102",
    "name": "Standard Twin",
    "type": "2 Giường Đơn",
    "capacity": 2,
    "sizeSqm": 0,
    "priceVnd": 1800000,
    "amenities": ["WiFi", "TV", "Air Conditioning"],
    "imageUrl": "https://example.com/room102.jpg",
    "popular": false,
    "rating": null,
    "reviews": null,
    "discount": null,
    "status": "available",
    "isVisible": true
  },
  {
    "id": 22,
    "roomNumber": "201",
    "name": "Deluxe Ocean View",
    "type": "1 Giường Đôi Lớn",
    "capacity": 2,
    "priceVnd": 2500000,
    "amenities": ["WiFi", "Ocean View", "Balcony"],
    "imageUrl": "https://example.com/room201.jpg",
    "status": "available",
    "isVisible": true
  }
]
```

### Empty Result

```json
[]
```

---

## 📝 Example Requests

### 1. Auto Recommendation (Default)

```bash
curl http://localhost:8080/api/rooms/recommend
```

```bash
curl http://localhost:8080/api/rooms/recommend?type=auto&limit=5
```

### 2. Popular Rooms

```bash
curl http://localhost:8080/api/rooms/recommend?type=popular&limit=3
```

### 3. Personalized for User

```bash
curl http://localhost:8080/api/rooms/recommend?accountId=123&type=personalized
```

### 4. Top Rated Rooms

```bash
curl http://localhost:8080/api/rooms/recommend?type=top_rated&limit=5
```

---

## 🧮 Algorithm Details

### 1️⃣ **Auto (Intelligent Fallback)**

**Priority Chain:**

1. **Personalized** (if `accountId` provided)
2. **Popular** (top booked)
3. **Top Rated** (highest ratings)
4. **Available** (fallback when no data)

**Logic:**

```java
if (accountId exists && user has booking history) {
    return rooms matching user's preferred bed layout
} else if (booking statistics exist) {
    return most booked rooms
} else if (review data exists) {
    return highest rated rooms
} else {
    return available rooms (sorted by price)
}
```

---

### 2️⃣ **Personalized**

**Algorithm:**

1. Query user's booking history
2. Find most frequently booked bed layout (e.g., "1 Giường Đôi Lớn")
3. Return available rooms with that bed layout
4. Sort by price (ascending)

**SQL Query (JPQL):**

```sql
SELECT b.room.bedLayout.id, COUNT(b.id)
FROM BookingEntity b
WHERE b.account.id = :accountId
  AND b.status IN ('confirmed', 'checked_in', 'checked_out')
GROUP BY b.room.bedLayout.id
ORDER BY COUNT(b.id) DESC
```

**Requirements:**

- User must have at least 1 completed booking
- Rooms must be `status = 'available'` and `is_visible = true`

**Fallback:**

- If no booking history → returns empty array
- Auto mode will cascade to popular/top_rated/available

---

### 3️⃣ **Popular (Top Booked)**

**Algorithm:**

1. Count total bookings per room
2. Sort by booking count (descending)
3. Filter only available & visible rooms

**SQL Query (JPQL):**

```sql
SELECT b.room.id, COUNT(b.id)
FROM BookingEntity b
WHERE b.status IN ('confirmed', 'checked_in', 'checked_out')
GROUP BY b.room.id
ORDER BY COUNT(b.id) DESC
```

**Requirements:**

- Database must have booking records
- Counts only confirmed/completed bookings

**Fallback:**

- If no bookings → returns empty array

---

### 4️⃣ **Top Rated**

**Algorithm:**

1. Calculate average rating per room from reviews
2. Sort by average rating (descending)
3. Filter only available & visible rooms

**SQL Query (Native):**

```sql
SELECT r.room_id, AVG(CAST(rv.rating AS FLOAT)) as avg_rating
FROM reviews rv
JOIN bookings b ON rv.booking_id = b.booking_id
JOIN rooms r ON b.room_id = r.room_id
GROUP BY r.room_id
ORDER BY avg_rating DESC
```

**Requirements:**

- Database must have review records
- Reviews linked to bookings and rooms

**Current Status:**
⚠️ **Partially Implemented** - Returns empty array if no reviews exist. Full implementation requires `ReviewRepository` integration.

---

## 🔍 Filtering & Visibility

All recommendation algorithms apply these filters:

1. **Status:** Only `status = 'available'`
2. **Visibility:** Only `is_visible = true`
3. **Limit:** Respects the `limit` parameter

**Code:**

```java
return recommendedRooms.stream()
    .filter(r -> Boolean.TRUE.equals(r.getIsVisible()))
    .limit(limit)
    .map(RoomMapper::toDto)
    .collect(Collectors.toList());
```

---

## ⚠️ Edge Cases & Fallbacks

| Scenario                 | Behavior                                                |
| ------------------------ | ------------------------------------------------------- |
| No booking data          | Popular → returns `[]`, Auto → cascades to next         |
| No review data           | Top Rated → returns `[]`, Auto → cascades to next       |
| User has no bookings     | Personalized → returns `[]`, Auto → cascades to popular |
| All algorithms fail      | Auto → returns available rooms sorted by price          |
| Invalid `type` parameter | Defaults to "auto"                                      |
| `limit` < 1              | Defaults to 5                                           |
| `limit` > database count | Returns all available rooms                             |

---

## 🧪 Testing

### Prerequisites

1. Backend running on `localhost:8080`
2. Database with sample rooms (`is_visible = true`, `status = 'available'`)

### Test Script

```powershell
.\test_recommend_api.ps1
```

### Manual Test Commands

**Test Auto (No User):**

```bash
curl http://localhost:8080/api/rooms/recommend
```

**Test Personalized (User ID = 1):**

```bash
curl http://localhost:8080/api/rooms/recommend?accountId=1&type=personalized
```

**Test Popular:**

```bash
curl http://localhost:8080/api/rooms/recommend?type=popular&limit=3
```

---

## 📊 Performance Considerations

### Database Queries

- **Personalized:** 1 query for user preferences + 1 for room list
- **Popular:** 1 aggregation query for booking counts + 1 for room details
- **Top Rated:** 1 aggregation query for ratings + 1 for room details
- **Available (Fallback):** 1 simple query

### Optimization Tips

1. **Index on `bookings.status`** for faster aggregation
2. **Index on `rooms.is_visible`** for filtering
3. **Cache popular/top_rated results** (update hourly/daily)
4. **Limit parameter** prevents excessive data transfer

### Recommended Limits

- Homepage carousel: `limit=5`
- Sidebar recommendations: `limit=3`
- Full recommendation page: `limit=10`

---

## 🚀 Future Enhancements

### Phase 2: Advanced Recommendations

1. **Collaborative Filtering** - "Users who booked this also booked..."
2. **Content-Based Filtering** - Match amenities, price range, location preferences
3. **Time-Based Recommendations** - Seasonal trends, weekend vs weekday
4. **Hybrid Model** - Combine multiple algorithms with weighted scores

### Phase 3: Machine Learning

1. **User Embeddings** - Learn user preferences from behavior
2. **A/B Testing** - Compare algorithm effectiveness
3. **Real-time Personalization** - Update recommendations based on current session

---

## 📞 Support

For issues or questions:

- Check `be_hotel/src/main/java/com/luxestay/hotel/service/impl/RoomServiceImpl.java`
- Review `test_recommend_api.ps1` for test scenarios
- Ensure database has sufficient sample data

---

## 📚 Related Documentation

- [Room Search API](./API_ROOM_SEARCH.md)
- [Room Availability API](./ROOM_AVAILABILITY_IMPLEMENTATION.md)
- [Room CRUD API](./ROOM_CRUD_IMPLEMENTATION.md)

