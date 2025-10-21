# II. Code Designs - Room Recommendation Feature

## 1. Room Recommendation (Gợi ý phòng)

_[Provide the detailed design for the function <Feature/Function Name>. It include Class Diagram, Class Specifications, and Sequence Diagram(s)]_

---

### a. Class Diagram

_[This part presents the class diagram for the relevant feature]_

```
┌─────────────────────────┐
│   RoomController        │
│─────────────────────────│
│ - roomService           │
│─────────────────────────│
│ + getRecommendedRooms() │
└───────────┬─────────────┘
            │ uses
            ▼
┌─────────────────────────┐
│   <<interface>>         │
│   RoomService           │
│─────────────────────────│
│ + recommendRooms()      │
└───────────┬─────────────┘
            │ implements
            ▼
┌─────────────────────────────────┐
│   RoomServiceImpl               │
│─────────────────────────────────│
│ - roomRepository                │
│ - bookingRepository             │
│ - bedLayoutRepository           │
│─────────────────────────────────│
│ + recommendRooms()              │
│ - getTopBookedRooms()           │
│ - getTopRatedRooms()            │
│ - getPersonalizedRooms()        │
│ - getAvailableRooms()           │
└──────┬──────────────┬───────────┘
       │ uses         │ uses
       ▼              ▼
┌──────────────┐  ┌──────────────────┐
│RoomRepository│  │BookingRepository │
│──────────────│  │──────────────────│
│+ findForList()│  │+ countBookings   │
│+ findAllById()│  │  ByRoom()        │
└──────────────┘  │+ findUserPrefer  │
                  │  redRoomTypes()  │
                  └──────────────────┘

┌─────────────────────────┐
│   RoomRecommendRequest  │  (DTO)
│─────────────────────────│
│ - accountId: Long       │
│ - type: String          │
│ - limit: Integer        │
│─────────────────────────│
│ + getters/setters       │
└─────────────────────────┘

┌─────────────────────────┐
│   Room                  │  (DTO)
│─────────────────────────│
│ - id: Long              │
│ - roomNumber: String    │
│ - name: String          │
│ - type: String          │
│ - capacity: int         │
│ - priceVnd: int         │
│ - amenities: String[]   │
│ - imageUrl: String      │
│ - status: String        │
│ - isVisible: Boolean    │
│─────────────────────────│
│ + getters/setters       │
└─────────────────────────┘
```

---

### b. Class Specifications

_[Provide the description for each class and the methods in each class, following the table format as below]_

#### RoomController Class

**Class Purpose:** REST API controller for handling room recommendation requests

| No  | Method                                        | Description                                                                                                                                                                                                                                                                                                 |
| --- | --------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `getRecommendedRooms(accountId, type, limit)` | **Input:** `accountId` (Long, optional), `type` (String, default "auto"), `limit` (Integer, default 5)<br>**Output:** `List<Room>`<br>**Processing:** Creates RoomRecommendRequest from parameters, calls roomService.recommendRooms(), returns list of recommended rooms filtered by visibility and status |

#### RoomService Interface

**Interface Purpose:** Service layer interface for room business logic including recommendation

| No  | Method                                     | Description                                                                                                                                                                                           |
| --- | ------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `recommendRooms(RoomRecommendRequest req)` | **Input:** `RoomRecommendRequest` (contains accountId, type, limit)<br>**Output:** `List<Room>`<br>**Processing:** Abstract method to be implemented by RoomServiceImpl for room recommendation logic |

#### RoomServiceImpl Class

**Class Purpose:** Implementation of room recommendation algorithms (auto, personalized, popular, top_rated)

| No  | Method                                     | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| --- | ------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 01  | `recommendRooms(RoomRecommendRequest req)` | **Input:** `RoomRecommendRequest` (accountId, type, limit)<br>**Output:** `List<Room>`<br>**Processing:** Main recommendation method. Based on `type` parameter:<br>- "auto": Try personalized → popular → top_rated → available<br>- "personalized": Get user's preferred room types<br>- "popular": Get most booked rooms<br>- "top_rated": Get highest rated rooms<br>Filters results by `isVisible=true`, `status='available'`, applies limit, maps entities to DTOs |
| 02  | `getTopBookedRooms(limit)`                 | **Input:** `limit` (Integer)<br>**Output:** `List<RoomEntity>`<br>**Processing:** Calls bookingRepository.countBookingsByRoom() to get booking statistics, extracts room IDs sorted by count (descending), fetches rooms by IDs, filters by status='available', returns top N rooms                                                                                                                                                                                      |
| 03  | `getTopRatedRooms(limit)`                  | **Input:** `limit` (Integer)<br>**Output:** `List<RoomEntity>`<br>**Processing:** ⚠️ Currently returns empty list. Future: will call reviewRepository to get average ratings per room, sort by rating (descending), return top N available rooms                                                                                                                                                                                                                         |
| 04  | `getPersonalizedRooms(accountId, limit)`   | **Input:** `accountId` (Integer), `limit` (Integer)<br>**Output:** `List<RoomEntity>`<br>**Processing:** Calls bookingRepository.findUserPreferredRoomTypes(accountId) to get user's most frequently booked bed layout, finds available rooms with that bed layout, sorts by price (ascending), returns top N rooms                                                                                                                                                      |
| 05  | `getAvailableRooms(limit)`                 | **Input:** `limit` (Integer)<br>**Output:** `List<RoomEntity>`<br>**Processing:** Fallback method. Calls roomRepository.findForList() with status='available', sorts by pricePerNight (ascending), returns top N rooms                                                                                                                                                                                                                                                   |

#### BookingRepository Interface

**Interface Purpose:** Data access layer for booking statistics used in recommendations

| No  | Method                                  | Description                                                                                                                                                                                                                                                                                                                                                                        |
| --- | --------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | `countBookingsByRoom()`                 | **Input:** None<br>**Output:** `List<Object[]>` where each array is [roomId, bookingCount]<br>**Processing:** JPQL aggregation query: Groups bookings by room_id, counts bookings with status IN ('confirmed', 'checked_in', 'checked_out'), orders by count DESC<br>**SQL:** See section (d) Database Queries                                                                     |
| 02  | `findUserPreferredRoomTypes(accountId)` | **Input:** `accountId` (Integer)<br>**Output:** `List<Object[]>` where each array is [bedLayoutId, bookingCount]<br>**Processing:** JPQL aggregation query: Groups user's bookings by bed_layout_id, counts bookings with status IN ('confirmed', 'checked_in', 'checked_out'), orders by count DESC to find most preferred room type<br>**SQL:** See section (d) Database Queries |

#### RoomRecommendRequest Class (DTO)

**Class Purpose:** Data Transfer Object for room recommendation request parameters

| No  | Field/Method      | Description                                                                      |
| --- | ----------------- | -------------------------------------------------------------------------------- |
| 01  | `accountId: Long` | User ID for personalized recommendations (optional, null for anonymous)          |
| 02  | `type: String`    | Algorithm type: "auto", "personalized", "popular", "top_rated" (default: "auto") |
| 03  | `limit: Integer`  | Number of rooms to return (default: 5, recommended max: 10)                      |
| 04  | Constructor       | Initializes with defaults: type="auto", limit=5                                  |
| 05  | Getters/Setters   | Standard accessor methods for all fields                                         |

#### Room Class (DTO)

**Class Purpose:** Data Transfer Object for room information returned to frontend

| No  | Field                 | Description                                                       |
| --- | --------------------- | ----------------------------------------------------------------- |
| 01  | `id: Long`            | Unique room identifier                                            |
| 02  | `roomNumber: String`  | Room number (e.g., "101", "102")                                  |
| 03  | `name: String`        | Room name (e.g., "Standard Twin", "Deluxe Ocean View")            |
| 04  | `type: String`        | Bed layout type (e.g., "1 Giường Đôi Lớn", "2 Giường Đơn")        |
| 05  | `capacity: int`       | Maximum number of guests                                          |
| 06  | `priceVnd: int`       | Price per night in VND                                            |
| 07  | `amenities: String[]` | Array of amenity names (e.g., ["WiFi", "TV", "Air Conditioning"]) |
| 08  | `imageUrl: String`    | URL to room's primary image                                       |
| 09  | `status: String`      | Room status: "available", "occupied", "maintenance"               |
| 10  | `isVisible: Boolean`  | Visibility flag (true = shown in search, false = hidden)          |

---

### c. Sequence Diagram(s)

_[Provide the sequence diagram(s) for the feature, see the sample below]_

#### Sequence Diagram 1: Auto Recommendation (No User Login)

```
Actor: User
Lifeline: Frontend (React)
Lifeline: RoomController
Lifeline: RoomServiceImpl
Lifeline: BookingRepository
Lifeline: RoomRepository

User -> Frontend: 1. Visit Homepage
Frontend -> RoomController: 2. GET /api/rooms/recommend?type=auto&limit=5
RoomController -> RoomController: 3. Create RoomRecommendRequest(null, "auto", 5)
RoomController -> RoomServiceImpl: 4. recommendRooms(request)

alt type = "auto" && accountId = null
    RoomServiceImpl -> BookingRepository: 5. countBookingsByRoom()
    BookingRepository --> RoomServiceImpl: 6. List<[roomId, count]>

    alt bookingStats exist
        RoomServiceImpl -> RoomServiceImpl: 7. Extract top room IDs
        RoomServiceImpl -> RoomRepository: 8. findAllById(topRoomIds)
        RoomRepository --> RoomServiceImpl: 9. List<RoomEntity>
        RoomServiceImpl -> RoomServiceImpl: 10. Filter by status='available'
    else no bookingStats
        RoomServiceImpl -> RoomRepository: 11. findForList(status='available')
        RoomRepository --> RoomServiceImpl: 12. List<RoomEntity>
    end
end

RoomServiceImpl -> RoomServiceImpl: 13. Filter by isVisible=true
RoomServiceImpl -> RoomServiceImpl: 14. Limit to 5 rooms
RoomServiceImpl -> RoomServiceImpl: 15. Map to Room DTOs
RoomServiceImpl --> RoomController: 16. List<Room>
RoomController --> Frontend: 17. JSON Response
Frontend -> Frontend: 18. Display in Carousel
Frontend --> User: 19. Show Recommended Rooms
```

#### Sequence Diagram 2: Personalized Recommendation (User Logged In)

```
Actor: User (accountId=123)
Lifeline: Frontend (React)
Lifeline: RoomController
Lifeline: RoomServiceImpl
Lifeline: BookingRepository
Lifeline: RoomRepository

User -> Frontend: 1. Visit Homepage (Logged In)
Frontend -> Frontend: 2. Get accountId from localStorage
Frontend -> RoomController: 3. GET /api/rooms/recommend?accountId=123&type=personalized
RoomController -> RoomController: 4. Create RoomRecommendRequest(123, "personalized", 5)
RoomController -> RoomServiceImpl: 5. recommendRooms(request)

RoomServiceImpl -> BookingRepository: 6. findUserPreferredRoomTypes(123)
BookingRepository --> RoomServiceImpl: 7. List<[bedLayoutId, count]>

alt user has booking history
    RoomServiceImpl -> RoomServiceImpl: 8. Extract most preferred bedLayoutId
    RoomServiceImpl -> RoomRepository: 9. findForList(status='available')
    RoomRepository --> RoomServiceImpl: 10. List<RoomEntity>
    RoomServiceImpl -> RoomServiceImpl: 11. Filter by bedLayoutId & isVisible=true
    RoomServiceImpl -> RoomServiceImpl: 12. Sort by price ASC
else user has no history
    RoomServiceImpl -> RoomServiceImpl: 13. Return empty list
    Note: Auto fallback will cascade to "popular"
end

RoomServiceImpl -> RoomServiceImpl: 14. Limit to 5 rooms
RoomServiceImpl -> RoomServiceImpl: 15. Map to Room DTOs
RoomServiceImpl --> RoomController: 16. List<Room>
RoomController --> Frontend: 17. JSON Response
Frontend -> Frontend: 18. Display in Carousel
Frontend --> User: 19. Show Personalized Rooms
```

#### Sequence Diagram 3: Popular Rooms

```
Actor: Admin/User
Lifeline: Frontend
Lifeline: RoomController
Lifeline: RoomServiceImpl
Lifeline: BookingRepository
Lifeline: RoomRepository

User -> Frontend: 1. Request Popular Rooms
Frontend -> RoomController: 2. GET /api/rooms/recommend?type=popular&limit=3
RoomController -> RoomServiceImpl: 3. recommendRooms(request)

RoomServiceImpl -> BookingRepository: 4. countBookingsByRoom()
BookingRepository --> RoomServiceImpl: 5. List<[roomId, bookingCount]>

RoomServiceImpl -> RoomServiceImpl: 6. Extract roomIds (sorted by count DESC)
RoomServiceImpl -> RoomRepository: 7. findAllById(topRoomIds)
RoomRepository --> RoomServiceImpl: 8. List<RoomEntity>

RoomServiceImpl -> RoomServiceImpl: 9. Filter by status='available' & isVisible=true
RoomServiceImpl -> RoomServiceImpl: 10. Limit to 3 rooms
RoomServiceImpl -> RoomServiceImpl: 11. Map to Room DTOs
RoomServiceImpl --> RoomController: 12. List<Room> (top 3 most booked)
RoomController --> Frontend: 13. JSON Response
Frontend --> User: 14. Display Popular Rooms
```

---

### d. Database Queries

_[Provide the detailed SQL (select, insert, update...) which are used in implementing the function/screen]_

#### Query 1: Count Bookings by Room (Popular Algorithm)

**Purpose:** Get booking statistics per room to identify most frequently booked rooms

**JPQL:**

```java
@Query("""
    SELECT b.room.id, COUNT(b.id)
    FROM BookingEntity b
    WHERE b.status IN ('confirmed', 'checked_in', 'checked_out')
    GROUP BY b.room.id
    ORDER BY COUNT(b.id) DESC
    """)
List<Object[]> countBookingsByRoom();
```

**Equivalent Native SQL:**

```sql
SELECT
    b.room_id,
    COUNT(b.booking_id) as booking_count
FROM bookings b
WHERE b.status IN ('confirmed', 'checked_in', 'checked_out')
GROUP BY b.room_id
ORDER BY booking_count DESC;
```

**Result Format:**

```
[
  [21, 25],  -- Room ID 21 has 25 bookings
  [22, 18],  -- Room ID 22 has 18 bookings
  [20, 12]   -- Room ID 20 has 12 bookings
]
```

**Usage:** Extract room IDs, fetch rooms by IDs, filter by status/visibility, return top N

---

#### Query 2: Find User's Preferred Room Types (Personalized Algorithm)

**Purpose:** Identify which bed layout (room type) the user books most frequently

**JPQL:**

```java
@Query("""
    SELECT b.room.bedLayout.id, COUNT(b.id)
    FROM BookingEntity b
    WHERE b.account.id = :accountId
      AND b.status IN ('confirmed', 'checked_in', 'checked_out')
    GROUP BY b.room.bedLayout.id
    ORDER BY COUNT(b.id) DESC
    """)
List<Object[]> findUserPreferredRoomTypes(@Param("accountId") Integer accountId);
```

**Equivalent Native SQL:**

```sql
SELECT
    r.bed_layout_id,
    COUNT(b.booking_id) as booking_count
FROM bookings b
JOIN rooms r ON b.room_id = r.room_id
WHERE b.account_id = :accountId
  AND b.status IN ('confirmed', 'checked_in', 'checked_out')
GROUP BY r.bed_layout_id
ORDER BY booking_count DESC;
```

**Example Input:** `accountId = 123`

**Result Format:**

```
[
  [1, 5],  -- Bed Layout ID 1 ("1 Giường Đôi Lớn") booked 5 times
  [2, 2],  -- Bed Layout ID 2 ("2 Giường Đơn") booked 2 times
  [3, 1]   -- Bed Layout ID 3 ("1 Giường Đôi") booked 1 time
]
```

**Usage:** Extract most preferred bed_layout_id (ID=1), find available rooms with that layout

---

#### Query 3: Find Available Rooms (Fallback & Base Query)

**Purpose:** Get available rooms sorted by price for fallback recommendation

**JPQL (via RoomRepository.findForList):**

```java
@Query("""
    SELECT DISTINCT r FROM RoomEntity r
    LEFT JOIN FETCH r.bedLayout
    WHERE r.status IN :statusList
      AND (:minPrice IS NULL OR r.pricePerNight >= :minPrice)
      AND (:maxPrice IS NULL OR r.pricePerNight <= :maxPrice)
    """)
Page<RoomEntity> findForList(
    @Param("statusList") List<String> statusList,
    @Param("layoutNames") List<String> layoutNames,
    @Param("minPrice") Integer minPrice,
    @Param("maxPrice") Integer maxPrice,
    @Param("q") String q,
    Pageable pageable
);
```

**Called with:**

```java
roomRepository.findForList(
    List.of("available"),  // statusList
    null,                  // layoutNames
    null,                  // minPrice
    null,                  // maxPrice
    null,                  // q
    PageRequest.of(0, limit, Sort.by("pricePerNight").ascending())
);
```

**Equivalent Native SQL:**

```sql
SELECT DISTINCT r.*, bl.*
FROM rooms r
LEFT JOIN bed_layouts bl ON r.bed_layout_id = bl.bed_layout_id
WHERE r.status = 'available'
ORDER BY r.price_per_night ASC
LIMIT :limit;
```

**Result:** List of RoomEntity objects, sorted by price (cheapest first)

---

#### Query 4: Find Rooms by IDs (For Popular Algorithm)

**Purpose:** Fetch room details for top booked room IDs

**JPA Method (inherited from JpaRepository):**

```java
List<RoomEntity> findAllById(Iterable<Integer> ids);
```

**Equivalent Native SQL:**

```sql
SELECT r.*, bl.*
FROM rooms r
LEFT JOIN bed_layouts bl ON r.bed_layout_id = bl.bed_layout_id
WHERE r.room_id IN (:roomIds);
```

**Example Input:** `roomIds = [21, 22, 20]` (from countBookingsByRoom, already sorted by popularity)

**Result:** List of RoomEntity objects in the order of input IDs

**Post-Processing:** Filter by status='available', isVisible=true, limit to N rooms

---

#### Query 5: Average Rating by Room (Top Rated Algorithm - Future)

**Purpose:** Calculate average rating per room from reviews

**Native SQL (ReviewRepository):**

```sql
@Query(value = """
    SELECT r.room_id, CAST(AVG(CAST(rv.rating AS FLOAT)) AS DECIMAL(3,2)) as avg_rating
    FROM reviews rv
    JOIN bookings b ON rv.booking_id = b.booking_id
    JOIN rooms r ON b.room_id = r.room_id
    GROUP BY r.room_id
    ORDER BY avg_rating DESC
    """, nativeQuery = true)
List<Object[]> findAvgRatingByRoom();
```

**Result Format:**

```
[
  [21, 4.85],  -- Room 21 has average rating 4.85
  [22, 4.60],  -- Room 22 has average rating 4.60
  [20, 4.20]   -- Room 20 has average rating 4.20
]
```

**Status:** ⚠️ Infrastructure ready, but currently returns empty (no review data). Will activate when reviews table is populated.

---

## Algorithm Decision Flow

```
START: recommendRooms(accountId, type, limit)
│
├─ IF type = "popular"
│   └─> countBookingsByRoom() → findAllById() → filter → return top N
│
├─ ELSE IF type = "top_rated"
│   └─> findAvgRatingByRoom() → findAllById() → filter → return top N
│      (Currently returns [] - future implementation)
│
├─ ELSE IF type = "personalized"
│   └─> IF accountId != null
│       └─> findUserPreferredRoomTypes(accountId) → findForList() → filter by bedLayout → return top N
│       ELSE
│       └─> return []
│
├─ ELSE (type = "auto" or invalid)
│   └─> TRY personalized (if accountId exists)
│       └─> IF empty: TRY popular
│           └─> IF empty: TRY top_rated
│               └─> IF empty: FALLBACK to available rooms
│
END: return List<Room> (filtered by isVisible=true, status='available', limited to N)
```

---

## Performance Considerations

### Indexes Required:

```sql
-- Index on bookings.status for faster aggregation
CREATE INDEX IX_bookings_status ON bookings(status);

-- Index on rooms.status for filtering
CREATE INDEX IX_rooms_status ON rooms(status);

-- Index on rooms.is_visible for filtering
CREATE INDEX IX_rooms_is_visible ON rooms(is_visible);

-- Composite index for common queries
CREATE INDEX IX_rooms_status_visible ON rooms(status, is_visible);
```

### Query Complexity:

| Algorithm    | Queries                | Complexity                  | Optimization        |
| ------------ | ---------------------- | --------------------------- | ------------------- |
| Popular      | 2 (aggregate + fetch)  | O(B log B) where B=bookings | Cache result hourly |
| Personalized | 2 (user stats + fetch) | O(U) where U=user bookings  | Cache per user      |
| Top Rated    | 2 (aggregate + fetch)  | O(R log R) where R=reviews  | Cache result hourly |
| Available    | 1 (simple select)      | O(N log N) where N=rooms    | No cache needed     |

### Recommended Cache Strategy:

```java
@Cacheable(value = "popularRooms", key = "#limit")
public List<RoomEntity> getTopBookedRooms(int limit) { ... }

@Cacheable(value = "topRatedRooms", key = "#limit")
public List<RoomEntity> getTopRatedRooms(int limit) { ... }

@Cacheable(value = "personalizedRooms", key = "#accountId + '-' + #limit")
public List<RoomEntity> getPersonalizedRooms(Integer accountId, int limit) { ... }
```

**Cache TTL:**

- Popular/Top Rated: 1 hour (stats don't change frequently)
- Personalized: 30 minutes (per user, updated after new booking)
- Clear cache on: new booking confirmed, review submitted

---

## Error Handling

### Edge Cases:

| Scenario                           | Behavior                         | HTTP Response             |
| ---------------------------------- | -------------------------------- | ------------------------- |
| No rooms available                 | Return `[]`                      | 200 OK with empty array   |
| Invalid `type` parameter           | Default to "auto"                | 200 OK (fallback)         |
| `limit` < 1                        | Default to 5                     | 200 OK                    |
| `accountId` not found              | Ignore personalization, use auto | 200 OK                    |
| Database connection error          | Throw exception                  | 500 Internal Server Error |
| All rooms hidden (isVisible=false) | Return `[]`                      | 200 OK with empty array   |

### Exception Handling:

```java
try {
    List<Object[]> stats = bookingRepository.countBookingsByRoom();
    // ... process stats
} catch (Exception e) {
    System.err.println("Error getting top booked rooms: " + e.getMessage());
    return new ArrayList<>(); // Return empty, let auto cascade
}
```

**Philosophy:** Fail gracefully. If one algorithm fails, auto mode cascades to next. Never throw exception to user for recommendation failures.

---

## Testing Scenarios

### Unit Tests (RoomServiceImplTest.java):

1. ✅ Test auto recommendation with no user (falls back to popular)
2. ✅ Test auto recommendation with user having booking history (uses personalized)
3. ✅ Test auto recommendation with user having NO history (falls back to popular)
4. ✅ Test popular algorithm with bookings
5. ✅ Test popular algorithm without bookings (returns empty)
6. ✅ Test personalized algorithm with user history
7. ✅ Test personalized algorithm without user history (returns empty)
8. ✅ Test top_rated algorithm (currently returns empty)
9. ✅ Test filtering by isVisible=true
10. ✅ Test filtering by status='available'
11. ✅ Test limit parameter (returns exactly N rooms)
12. ✅ Test invalid type defaults to auto

### Integration Tests (PowerShell script):

- See `test_recommend_api.ps1` for API endpoint testing

---

## API Contract

**Endpoint:** `GET /api/rooms/recommend`

**Request:**

```
GET /api/rooms/recommend?accountId=123&type=auto&limit=5
```

**Response (200 OK):**

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
  }
]
```

**Response (Empty):**

```json
[]
```

**No Error Responses** - Always returns 200 OK with array (empty if no recommendations)

---

**End of Room Recommendation Design Document**

