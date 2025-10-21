# II. Code Designs - Room Detail Feature

## 1. View Detail (Xem chi tiết phòng)

_[Provide the detailed design for the function <Feature/Function Name>. It include Class Diagram, Class Specifications, and Sequence Diagram(s)]_

---

### a. Class Diagram

_[This part presents the class diagram for the relevant feature]_

**![Class Diagram - Room Detail](room_detail_class_diagram.png)**

_Note: Export PlantUML diagram from `be_hotel/docs/diagrams/room_detail_class_diagram.puml` và insert image vào đây_

---

### b. Class Specifications

_[Provide the description for each class and the methods in each class, following the table format as below]_

#### RoomDetail

| No  | Method             | Description                                                          |
| --- | ------------------ | -------------------------------------------------------------------- |
| 01  | id                 | Unique room identifier (primary key).                                |
| 02  | roomNumber         | Room number displayed to users.                                      |
| 03  | name               | Room name or title.                                                  |
| 04  | type               | Bed layout type (e.g., "1 Giường Đôi Lớn", "2 Giường Đơn").          |
| 05  | priceVnd           | Price per night in Vietnamese Dong.                                  |
| 06  | capacity           | Maximum number of guests.                                            |
| 07  | imageUrl           | Primary room image URL.                                              |
| 08  | images             | Array of additional image URLs.                                      |
| 09  | description        | Detailed room description.                                           |
| 10  | highlights         | Array of room highlights (e.g., ["View biển", "Gần trung tâm"]).     |
| 11  | amenities          | Array of amenity names grouped by category.                          |
| 12  | avgRating          | Average rating from customer reviews (0.0 - 5.0).                    |
| 13  | totalReviews       | Total number of reviews.                                             |
| 14  | ratingDistribution | Distribution of ratings by star (5-star: count, 4-star: count, ...). |
| 15  | reviews            | Array of recent reviews with reviewer info, rating, comment, date.   |

#### RoomController

| No  | Method            | Description                                                                                                                                                                                                                                                          |
| --- | ----------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | roomService       | Dependency injection of RoomService interface.                                                                                                                                                                                                                       |
| 02  | getRoomDetail(id) | **Input:** id (Long, room identifier)<br>**Output:** ResponseEntity&lt;RoomDetail&gt; - HTTP 200 OK with room detail JSON<br>**Processing:** Calls roomService.getRoomDetail(), returns detailed room information including amenities, photos, ratings, and reviews. |

#### RoomService (Interface)

| No  | Method            | Description                                                                                                                                                      |
| --- | ----------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | getRoomDetail(id) | **Input:** id (Long, room identifier)<br>**Output:** RoomDetail DTO<br>**Processing:** Abstract method for fetching detailed room information with related data. |

#### RoomServiceImpl

| No  | Method            | Description                                                                                                                                                                                                                                                                                                                                                                              |
| --- | ----------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | roomRepository    | Dependency injection of RoomRepository.                                                                                                                                                                                                                                                                                                                                                  |
| 02  | reviewRepository  | Dependency injection of ReviewRepository for fetching ratings and reviews.                                                                                                                                                                                                                                                                                                               |
| 03  | getRoomDetail(id) | **Input:** id (Long)<br>**Output:** RoomDetail DTO<br>**Processing:** Fetches room entity via roomRepository.findById(), throws exception if not found, fetches related data (amenities, images, bed layout), calculates average rating and rating distribution from reviews, fetches recent reviews (limit 10), maps all data to RoomDetail DTO with complete information, returns DTO. |

#### RoomRepository (Interface)

| No  | Method       | Description                                                                                                                                                               |
| --- | ------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | findById(id) | **Input:** id (Long, room identifier)<br>**Output:** Optional&lt;RoomEntity&gt;<br>**Processing:** JPA method. Fetches room with eager loading of bedLayout relationship. |

#### ReviewRepository (Interface)

| No  | Method                          | Description                                                                                                                                                                                                                                                                 |
| --- | ------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | findByRoomIdWithAccount(roomId) | **Input:** roomId (Integer)<br>**Output:** List&lt;ReviewEntity&gt;<br>**Processing:** Custom query. Fetches reviews for specific room with JOIN on bookings and accounts to get reviewer information. Orders by created date descending, limits to 10 most recent reviews. |

---

### c. Sequence Diagram

_[Provide the sequence diagram(s) for the feature, see the sample below]_

**![Sequence Diagram - Room Detail](room_detail_sequence.png)**

_Note: Export PlantUML diagram from `be_hotel/docs/diagrams/room_detail_sequence.puml` và insert image vào đây_

**Flow Description:**

This sequence diagram shows the flow when a user views room details.

#### Main Flow:

1. **Navigation:**

   - User browses search results or homepage
   - User clicks on room card or "Xem chi tiết" button
   - Frontend navigates to `/rooms/{id}` route

2. **Data Fetching:**

   - Frontend extracts room ID from URL params
   - Frontend sends GET request to `/api/rooms/{id}`
   - RoomController receives request (no authorization required - public endpoint)
   - Controller calls RoomServiceImpl.getRoomDetail(id)

3. **Room Data Retrieval:**

   - Service calls RoomRepository.findById(id)
   - Repository queries database: `SELECT * FROM rooms WHERE room_id = ? (with JOIN bed_layouts)`
   - If room not found → throws exception, returns HTTP 404 Not Found
   - If found → Repository returns RoomEntity with bed layout information

4. **Review Data Retrieval:**

   - Service calls ReviewRepository.findByRoomIdWithAccount(id)
   - Repository executes complex query:
     - JOIN reviews → bookings → accounts
     - Filters by room_id
     - Calculates average rating
     - Gets rating distribution (count per star)
     - Fetches recent 10 reviews with reviewer info
   - Repository returns review data

5. **Data Mapping:**

   - Service maps RoomEntity to RoomDetail DTO
   - Sets basic fields: id, roomNumber, name, type, price, capacity
   - Sets images: imageUrl, additional images array
   - Sets content: description, highlights
   - Groups amenities by category (Tiện nghi phòng, Phòng tắm, Giải trí, etc.)
   - Sets rating info: avgRating, totalReviews, ratingDistribution
   - Maps reviews with: reviewerId, reviewerName, avatarUrl, rating, comment, createdDate

6. **Response:**
   - Service returns RoomDetail DTO to Controller
   - Controller returns HTTP 200 OK with complete room detail JSON
   - Frontend receives response and renders UI:
     - Image carousel/gallery
     - Room information (name, price, capacity, type)
     - Description and highlights
     - Amenities grouped by category with icons
     - Rating histogram (5-star, 4-star, 3-star, 2-star, 1-star distribution)
     - Review list with reviewer info, rating stars, comment, date
   - User sees complete room detail page

#### Error Handling Summary:

| Error Type     | HTTP Status   | User Message           |
| -------------- | ------------- | ---------------------- |
| Room Not Found | 404 Not Found | "Không tìm thấy phòng" |

---

### d. Database Queries

_[Provide the detailed SQL (select, insert, update...) which are used in implementing the function/screen]_

**Purpose:** Database queries used in room detail view including room info, amenities, photos, and reviews.

**SQL Queries:**

```sql
-- Query 1: Get room details with bed layout
SELECT
    r.room_id,
    r.room_number,
    r.room_name,
    r.price_per_night,
    r.capacity,
    r.description,
    r.amenities,
    r.image_url,
    r.status,
    r.is_visible,
    bl.bed_layout_id,
    bl.layout_name AS type,
    bl.description AS type_description
FROM rooms r
LEFT JOIN bed_layouts bl ON r.bed_layout_id = bl.bed_layout_id
WHERE r.room_id = 20;  -- Thay 20 bằng room ID cụ thể

-- Query 2: Get average rating for room
SELECT
    r.room_id,
    COUNT(rv.review_id) AS total_reviews,
    CAST(AVG(CAST(rv.rating AS FLOAT)) AS DECIMAL(3,2)) AS avg_rating
FROM rooms r
LEFT JOIN bookings b ON r.room_id = b.room_id
LEFT JOIN reviews rv ON b.booking_id = rv.booking_id
WHERE r.room_id = 20
GROUP BY r.room_id;

-- Query 3: Get rating distribution
SELECT
    rv.rating,
    COUNT(rv.review_id) AS count
FROM reviews rv
JOIN bookings b ON rv.booking_id = b.booking_id
JOIN rooms r ON b.room_id = r.room_id
WHERE r.room_id = 20
GROUP BY rv.rating
ORDER BY rv.rating DESC;

-- Query 4: Get recent reviews with reviewer info
SELECT TOP 10
    rv.review_id,
    rv.rating,
    rv.comment,
    rv.created_at,
    a.account_id AS reviewer_id,
    a.full_name AS reviewer_name,
    a.avatar_url
FROM reviews rv
JOIN bookings b ON rv.booking_id = b.booking_id
JOIN accounts a ON b.account_id = a.account_id
WHERE b.room_id = 20
ORDER BY rv.created_at DESC;
```

**Explanation:**

- **Query 1 (Room Details)**: Fetches complete room information with bed layout via LEFT JOIN
- **Query 2 (Average Rating)**: Calculates average rating and total review count for the room
- **Query 3 (Rating Distribution)**: Groups reviews by star rating to create histogram data
- **Query 4 (Recent Reviews)**: Fetches 10 most recent reviews with reviewer information (name, avatar)

**Result Example:**

| Query | Result                                                                              |
| ----- | ----------------------------------------------------------------------------------- |
| 1     | Room data with type: id=20, number="101", name="Standard Twin", type="2 Giường Đơn" |
| 2     | avg_rating=4.5, total_reviews=32                                                    |
| 3     | {5: 15, 4: 10, 3: 5, 2: 1, 1: 1}                                                    |
| 4     | List of 10 reviews with reviewer info, rating, comment, date                        |

---

## Data Structure Summary

**RoomDetail DTO Structure:**

```json
{
  "id": 20,
  "roomNumber": "101",
  "name": "Standard Twin",
  "type": "2 Giường Đơn",
  "priceVnd": 1500000,
  "capacity": 2,
  "imageUrl": "https://example.com/room101-main.jpg",
  "images": [
    "https://example.com/room101-1.jpg",
    "https://example.com/room101-2.jpg",
    "https://example.com/room101-3.jpg"
  ],
  "description": "Phòng tiêu chuẩn với 2 giường đơn, view thành phố...",
  "highlights": ["View thành phố", "Gần trung tâm", "Wifi tốc độ cao"],
  "amenities": [
    "WiFi miễn phí",
    "TV màn hình phẳng",
    "Điều hòa không khí",
    "Mini Bar",
    "Két an toàn",
    "Máy sấy tóc"
  ],
  "avgRating": 4.5,
  "totalReviews": 32,
  "ratingDistribution": {
    "5": 15,
    "4": 10,
    "3": 5,
    "2": 1,
    "1": 1
  },
  "reviews": [
    {
      "reviewId": 123,
      "reviewerId": 45,
      "reviewerName": "Nguyễn Văn A",
      "avatarUrl": "https://example.com/avatar45.jpg",
      "rating": 5,
      "comment": "Phòng rất sạch sẽ và thoải mái...",
      "createdDate": "2025-10-15"
    }
  ]
}
```

---

**End of Room Detail Design Document**
