# II. Code Designs - Room Recommendation Feature

## 1. Room Recommendation (Gợi ý phòng)

_[Provide the detailed design for the function <Feature/Function Name>. It include Class Diagram, Class Specifications, and Sequence Diagram(s)]_

---

### a. Class Diagram

_[This part presents the class diagram for the relevant feature]_

**![Class Diagram - Room Recommendation](room_recommend_class_diagram.png)**

_Note: Export PlantUML diagram from `be_hotel/docs/diagrams/room_recommend_class_diagram.puml` và insert image vào đây_

---

### b. Class Specifications

_[Provide the description for each class and the methods in each class, following the table format as below]_

#### RoomRecommendRequest

| No  | Method      | Description                                                                                                      |
| --- | ----------- | ---------------------------------------------------------------------------------------------------------------- |
| 01  | accountId   | User ID for personalized recommendations (optional, null for anonymous users).                                   |
| 02  | type        | Algorithm type: "auto", "personalized", "popular", "top_rated" (default: "auto").                                |
| 03  | limit       | Number of rooms to return (default: 5, recommended max: 10).                                                     |
| 04  | getters     | Standard getter methods to retrieve field values (getAccountId(), getType(), getLimit()).                        |
| 05  | setters     | Standard setter methods to update field values (setAccountId(), setType(), setLimit()).                          |
| 06  | constructor | Default constructor initializes type="auto" and limit=5. Parameterized constructor accepts all three parameters. |

#### Room

| No  | Method     | Description                                                                                                  |
| --- | ---------- | ------------------------------------------------------------------------------------------------------------ |
| 01  | id         | Unique room identifier (primary key).                                                                        |
| 02  | roomNumber | Room number displayed to users (e.g., "101", "102", "201").                                                  |
| 03  | name       | Room name or title (e.g., "Standard Twin", "Deluxe Ocean View").                                             |
| 04  | type       | Bed layout type describing room configuration (e.g., "1 Giường Đôi Lớn", "2 Giường Đơn").                    |
| 05  | capacity   | Maximum number of guests the room can accommodate.                                                           |
| 06  | priceVnd   | Price per night in Vietnamese Dong (VND).                                                                    |
| 07  | amenities  | Array of amenity names available in the room (e.g., ["WiFi", "TV", "Air Conditioning", "Mini Bar"]).         |
| 08  | imageUrl   | URL to the room's primary display image.                                                                     |
| 09  | status     | Current room status: "available" (ready for booking), "occupied" (currently in use), "maintenance" (closed). |
| 10  | isVisible  | Visibility flag: true = shown in search results, false = hidden from public view.                            |
| 11  | getters    | Standard getter methods for all fields.                                                                      |
| 12  | setters    | Standard setter methods for all fields.                                                                      |

#### RoomController

| No  | Method                                      | Description                                                                                                                                                                                                                                                                                                                  |
| --- | ------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | roomService                                 | Dependency injection of RoomService interface for business logic processing.                                                                                                                                                                                                                                                 |
| 02  | getRecommendedRooms(accountId, type, limit) | **Input:** accountId (Long, optional), type (String, default "auto"), limit (Integer, default 5)<br>**Output:** List&lt;Room&gt; - JSON array of recommended rooms<br>**Processing:** Extracts request parameters, creates RoomRecommendRequest DTO, calls roomService.recommendRooms(), returns filtered room list as JSON. |

#### RoomService (Interface)

| No  | Method                                   | Description                                                                                                                                                                                                    |
| --- | ---------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | recommendRooms(RoomRecommendRequest req) | **Input:** RoomRecommendRequest (contains accountId, type, limit)<br>**Output:** List&lt;Room&gt;<br>**Processing:** Abstract method defining room recommendation contract to be implemented by service layer. |

#### RoomServiceImpl

| No  | Method                                   | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| --- | ---------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | roomRepository                           | Dependency injection of RoomRepository for room data access.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| 02  | bookingRepository                        | Dependency injection of BookingRepository for booking statistics access.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| 03  | bedLayoutRepository                      | Dependency injection of BedLayoutRepository for bed layout data access.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| 04  | recommendRooms(RoomRecommendRequest req) | **Input:** RoomRecommendRequest (accountId, type, limit)<br>**Output:** List&lt;Room&gt;<br>**Processing:** Main recommendation orchestrator. Routes to appropriate algorithm based on type parameter: "auto" = cascading fallback (personalized → popular → top_rated → available), "personalized" = user booking history based, "popular" = most frequently booked, "top_rated" = highest average rating. Applies filtering (isVisible=true, status='available'), pagination (limit), and entity-to-DTO mapping before returning results.                    |
| 05  | getTopBookedRooms(limit)                 | **Input:** limit (Integer)<br>**Output:** List&lt;RoomEntity&gt;<br>**Processing:** Retrieves booking statistics via bookingRepository.countBookingsByRoom(), extracts room IDs sorted by booking count descending, fetches corresponding RoomEntity objects via roomRepository.findAllById(), filters by status='available', returns top N most frequently booked rooms.                                                                                                                                                                                      |
| 06  | getTopRatedRooms(limit)                  | **Input:** limit (Integer)<br>**Output:** List&lt;RoomEntity&gt;<br>**Processing:** ⚠️ Currently returns empty list as placeholder. Future implementation will: retrieve average ratings per room from reviewRepository.findAvgRatingByRoom(), sort rooms by rating descending, fetch room details, filter by status='available', return top N highest-rated rooms. Infrastructure prepared for review system integration.                                                                                                                                     |
| 07  | getPersonalizedRooms(accountId, limit)   | **Input:** accountId (Integer), limit (Integer)<br>**Output:** List&lt;RoomEntity&gt;<br>**Processing:** Analyzes user preferences via bookingRepository.findUserPreferredRoomTypes(accountId) which returns bed layouts grouped by booking frequency, extracts most preferred bed layout ID (highest count), queries roomRepository.findForList() with status='available' filter, further filters results by matching bed layout ID and isVisible=true, sorts by pricePerNight ascending (budget-friendly first), returns top N personalized recommendations. |
| 08  | getAvailableRooms(limit)                 | **Input:** limit (Integer)<br>**Output:** List&lt;RoomEntity&gt;<br>**Processing:** Fallback method for when no statistical data exists. Queries roomRepository.findForList() with status='available', sorts by pricePerNight ascending to prioritize affordable options, limits results to N rooms, returns basic available room list without personalization.                                                                                                                                                                                                |

#### RoomRepository (Interface)

| No  | Method                                                                | Description                                                                                                                                                                                                                                                                                                                                                                                      |
| --- | --------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 01  | findForList(statusList, layoutNames, minPrice, maxPrice, q, pageable) | **Input:** statusList (List&lt;String&gt;), layoutNames (List&lt;String&gt;), minPrice (Integer), maxPrice (Integer), q (String search query), pageable (Pageable)<br>**Output:** Page&lt;RoomEntity&gt;<br>**Processing:** JPQL query with dynamic filtering by status, layout, price range, and search term. Supports pagination and sorting. Returns page of room entities matching criteria. |
| 02  | findAllById(ids)                                                      | **Input:** ids (Iterable&lt;Integer&gt;)<br>**Output:** List&lt;RoomEntity&gt;<br>**Processing:** JPA built-in method. Fetches room entities by list of room IDs. Used to retrieve details of top booked/rated rooms after statistics aggregation.                                                                                                                                               |

#### BookingRepository (Interface)

| No  | Method                                | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| --- | ------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | countBookingsByRoom()                 | **Input:** None<br>**Output:** List&lt;Object[]&gt; where each array element is [roomId (Integer), bookingCount (Long)]<br>**Processing:** JPQL aggregation query: `SELECT b.room.id, COUNT(b.id) FROM BookingEntity b WHERE b.status IN ('confirmed', 'checked_in', 'checked_out') GROUP BY b.room.id ORDER BY COUNT(b.id) DESC`. Groups all confirmed/active bookings by room, counts occurrences, orders by popularity descending. Used for "popular" algorithm.                                                        |
| 02  | findUserPreferredRoomTypes(accountId) | **Input:** accountId (Integer)<br>**Output:** List&lt;Object[]&gt; where each array element is [bedLayoutId (Integer), bookingCount (Long)]<br>**Processing:** JPQL aggregation query: `SELECT b.room.bedLayout.id, COUNT(b.id) FROM BookingEntity b WHERE b.account.id = :accountId AND b.status IN ('confirmed', 'checked_in', 'checked_out') GROUP BY b.room.bedLayout.id ORDER BY COUNT(b.id) DESC`. Analyzes user's booking history to identify most frequently booked room types. Used for "personalized" algorithm. |

---

### c. Sequence Diagram(s)

_[Provide the sequence diagram(s) for the feature, see the sample below]_

#### Sequence Diagram 1: Auto Recommendation (No User Login)

**![Sequence Diagram - Auto Recommendation](room_recommend_sequence_auto.png)**

_Note: Export PlantUML diagram from `be_hotel/docs/diagrams/room_recommend_sequence_auto.puml` và insert image vào đây_

**Flow Description:**

1. User visits homepage without logging in
2. Frontend sends GET request to `/api/rooms/recommend?type=auto&limit=5`
3. RoomController receives request and creates RoomRecommendRequest object with accountId=null, type="auto", limit=5
4. Controller delegates to RoomServiceImpl.recommendRooms(request)
5. Service detects type="auto" and accountId=null, initiates fallback cascade
6. First attempt: Calls BookingRepository.countBookingsByRoom() to retrieve booking statistics
7. If booking data exists:
   - Service extracts top room IDs sorted by booking count descending
   - Calls RoomRepository.findAllById(topRoomIds) to fetch room details
   - Filters results by status='available' and isVisible=true
8. If no booking data exists:
   - Falls back to RoomRepository.findForList(status='available')
   - Retrieves basic available rooms sorted by price
9. Service applies limit (5 rooms maximum)
10. Maps RoomEntity objects to Room DTOs
11. Returns List&lt;Room&gt; to Controller
12. Controller serializes to JSON and returns HTTP 200 OK response
13. Frontend receives room list and displays in carousel component
14. User sees 5 recommended rooms on homepage

---

#### Sequence Diagram 2: Personalized Recommendation (User Logged In)

**![Sequence Diagram - Personalized Recommendation](room_recommend_sequence_personalized.png)**

_Note: Export PlantUML diagram from `be_hotel/docs/diagrams/room_recommend_sequence_personalized.puml` và insert image vào đây_

**Flow Description:**

1. Logged-in user (accountId=123) visits homepage
2. Frontend retrieves accountId from localStorage (or session)
3. Frontend sends GET request to `/api/rooms/recommend?accountId=123&type=personalized&limit=5`
4. RoomController creates RoomRecommendRequest(123, "personalized", 5)
5. Controller delegates to RoomServiceImpl.recommendRooms(request)
6. Service calls BookingRepository.findUserPreferredRoomTypes(123)
7. Repository returns list of bed layouts grouped by booking frequency: [[layoutId=1, count=5], [layoutId=2, count=2], ...]
8. If user has booking history (list not empty):
   - Service extracts most preferred bed layout ID (layoutId=1, highest count)
   - Calls RoomRepository.findForList(status='available') to get all available rooms
   - Filters results by: bedLayoutId=1 AND isVisible=true
   - Sorts filtered rooms by pricePerNight ascending (budget-friendly)
   - Limits to 5 rooms
9. If user has no booking history (empty list):
   - Service returns empty list
   - Note: Auto mode will cascade to "popular" algorithm as fallback
10. Service maps RoomEntity objects to Room DTOs
11. Returns personalized List&lt;Room&gt; to Controller
12. Controller returns JSON response
13. Frontend displays personalized recommendations in carousel
14. User sees rooms matching their historical preferences

---

#### Sequence Diagram 3: Popular Rooms

**![Sequence Diagram - Popular Rooms](room_recommend_sequence_popular.png)**

_Note: Export PlantUML diagram from `be_hotel/docs/diagrams/room_recommend_sequence_popular.puml` và insert image vào đây_

**Flow Description:**

1. User (admin or regular user) requests popular rooms
2. Frontend sends GET request to `/api/rooms/recommend?type=popular&limit=3`
3. RoomController creates RoomRecommendRequest(null, "popular", 3)
4. Controller delegates to RoomServiceImpl.recommendRooms(request)
5. Service calls BookingRepository.countBookingsByRoom()
6. Repository returns booking statistics: [[roomId=21, count=25], [roomId=22, count=18], [roomId=20, count=12], ...]
7. Service extracts room IDs from result (already sorted by count descending)
8. Service calls RoomRepository.findAllById([21, 22, 20, ...])
9. Repository returns corresponding RoomEntity objects
10. Service filters results by status='available' AND isVisible=true
11. Service limits to top 3 rooms
12. Service maps RoomEntity objects to Room DTOs
13. Returns top 3 most booked rooms to Controller
14. Controller returns JSON response
15. Frontend displays popular rooms (e.g., in "Most Popular" section)
16. User sees the 3 most frequently booked rooms

---

### d. Database Queries

_[Provide the detailed SQL (select, insert, update...) which are used in implementing the function/screen]_

#### Query 1: Count Bookings by Room (Popular Algorithm)

**Purpose:** Aggregate booking statistics per room to identify most frequently booked rooms for "popular" recommendation.

**SQL Query:**

```sql
-- Đếm số lượng booking theo từng phòng
SELECT
    b.room_id,
    COUNT(b.booking_id) AS booking_count
FROM bookings b
WHERE b.status IN ('confirmed', 'checked_in', 'checked_out')
GROUP BY b.room_id
ORDER BY booking_count DESC;
```

**Explanation:**

- Filters bookings with confirmed/active statuses (excludes cancelled/pending)
- Groups by room_id to count bookings per room
- Orders by booking count descending (most popular first)
- Returns list of [room_id, count] for top N rooms

**Result Example:**

| room_id | booking_count |
| ------- | ------------- |
| 21      | 25            |
| 22      | 18            |
| 20      | 12            |

---

#### Query 2: Find User's Preferred Room Types (Personalized Algorithm)

**Purpose:** Analyze user's booking history to identify most frequently booked bed layout (room type) for personalized recommendations.

**SQL Query:**

```sql
-- Tìm loại giường người dùng thích dựa trên lịch sử booking
SELECT
    r.bed_layout_id,
    COUNT(b.booking_id) AS booking_count
FROM bookings b
JOIN rooms r ON b.room_id = r.room_id
WHERE b.account_id = 123  -- Thay 123 bằng accountId cụ thể
  AND b.status IN ('confirmed', 'checked_in', 'checked_out')
GROUP BY r.bed_layout_id
ORDER BY booking_count DESC;
```

**Explanation:**

- Filters bookings by specific user (replace 123 with actual accountId)
- Joins with rooms table to access bed_layout_id
- Groups by bed_layout_id to count bookings per room type
- Orders by booking count descending (most preferred first)
- Returns user's room type preferences

**Result Example (accountId=123):**

| bed_layout_id | booking_count |
| ------------- | ------------- |
| 1             | 5             |
| 2             | 2             |
| 3             | 1             |

---

#### Query 3: Find Available Rooms with Filters (Fallback Query)

**Purpose:** Retrieve available rooms with optional filtering by price range. Used as fallback when no personalized data exists.

**SQL Query:**

```sql
-- Lấy danh sách phòng available với filters
SELECT DISTINCT
    r.room_id,
    r.room_number,
    r.room_name,
    r.price_per_night,
    r.capacity,
    r.status,
    r.is_visible,
    r.image_url,
    r.amenities,
    r.bed_layout_id,
    bl.layout_name,
    bl.description
FROM rooms r
LEFT JOIN bed_layouts bl ON r.bed_layout_id = bl.bed_layout_id
WHERE r.status = 'available'
  AND r.is_visible = 1
  AND (r.price_per_night >= 1000000 OR 1000000 IS NULL)  -- minPrice (optional)
  AND (r.price_per_night <= 10000000 OR 10000000 IS NULL)  -- maxPrice (optional)
ORDER BY r.price_per_night ASC;
```

**Explanation:**

- LEFT JOIN with bed_layouts to get room type information
- Filters by status='available' and is_visible=1
- Optional price range filtering (remove OR condition if not needed)
- Orders by price ascending (affordable rooms first)
- Returns list of available rooms with complete details

**Result Example:**

| room_id | room_number | room_name     | price_per_night | capacity | layout_name      |
| ------- | ----------- | ------------- | --------------- | -------- | ---------------- |
| 20      | 101         | Standard Twin | 1500000         | 2        | 2 Giường Đơn     |
| 21      | 102         | Deluxe Double | 2500000         | 2        | 1 Giường Đôi Lớn |
| 22      | 201         | Family Suite  | 3500000         | 4        | 2 Giường Đôi     |

---

#### Query 4: Find Rooms by IDs (Bulk Fetch for Popular Rooms)

**Purpose:** Efficiently retrieve multiple room details by list of room IDs after statistics aggregation.

**SQL Query:**

```sql
-- Lấy chi tiết phòng theo danh sách room IDs
SELECT
    r.room_id,
    r.room_number,
    r.room_name,
    r.price_per_night,
    r.capacity,
    r.status,
    r.is_visible,
    r.image_url,
    r.amenities,
    r.bed_layout_id,
    bl.layout_name
FROM rooms r
LEFT JOIN bed_layouts bl ON r.bed_layout_id = bl.bed_layout_id
WHERE r.room_id IN (21, 22, 20);  -- Danh sách room IDs từ Query 1
```

**Explanation:**

- Fetches room details for specific room IDs
- Uses IN clause for bulk retrieval (efficient for multiple IDs)
- Joins with bed_layouts to get room type information
- Used after Query 1 to get full details of top booked rooms

**Result Example:**

| room_id | room_number | room_name     | price_per_night | status    | layout_name      |
| ------- | ----------- | ------------- | --------------- | --------- | ---------------- |
| 21      | 102         | Deluxe Double | 2500000         | available | 1 Giường Đôi Lớn |
| 22      | 201         | Family Suite  | 3500000         | available | 2 Giường Đôi     |
| 20      | 101         | Standard Twin | 1500000         | available | 2 Giường Đơn     |

---

#### Query 5: Average Rating by Room (Top Rated Algorithm - Future)

**Purpose:** Calculate average rating per room from customer reviews for "top_rated" recommendation algorithm.

**SQL Query:**

```sql
-- Tính điểm đánh giá trung bình cho mỗi phòng
SELECT
    r.room_id,
    r.room_number,
    r.room_name,
    CAST(AVG(CAST(rv.rating AS FLOAT)) AS DECIMAL(3,2)) AS avg_rating,
    COUNT(rv.review_id) AS review_count
FROM reviews rv
JOIN bookings b ON rv.booking_id = b.booking_id
JOIN rooms r ON b.room_id = r.room_id
GROUP BY r.room_id, r.room_number, r.room_name
HAVING COUNT(rv.review_id) >= 3  -- Chỉ lấy phòng có ít nhất 3 reviews
ORDER BY avg_rating DESC, review_count DESC;
```

**Explanation:**

- Joins reviews → bookings → rooms to link reviews with rooms
- Calculates average rating per room (cast to FLOAT for precision)
- Counts number of reviews per room for reliability
- HAVING clause filters rooms with at least 3 reviews (statistical significance)
- Orders by average rating descending, then by review count
- Returns top rated rooms with sufficient review data

**Result Example:**

| room_id | room_number | room_name     | avg_rating | review_count |
| ------- | ----------- | ------------- | ---------- | ------------ |
| 21      | 102         | Deluxe Double | 4.85       | 45           |
| 22      | 201         | Family Suite  | 4.60       | 32           |
| 20      | 101         | Standard Twin | 4.20       | 28           |

**Status:** ⚠️ Infrastructure prepared but currently inactive. Will be enabled when review system is populated with customer feedback.

---

**End of Room Recommendation Design Document**
