# II. Code Designs - Find Room Feature

## 1. Find Room (Tìm phòng trống)

_[Provide the detailed design for the function <Feature/Function Name>. It include Class Diagram, Class Specifications, and Sequence Diagram(s)]_

---

### a. Class Diagram

_[This part presents the class diagram for the relevant feature]_

**![Class Diagram - Find Room](find_room_class_diagram.png)**

_Note: Export PlantUML diagram from `be_hotel/docs/diagrams/find_room_class_diagram.puml` và insert image vào đây_

---

### b. Class Specifications

_[Provide the description for each class and the methods in each class, following the table format as below]_

#### RoomAvailabilityRequest

| No  | Method   | Description                                         |
| --- | -------- | --------------------------------------------------- |
| 01  | checkIn  | Check-in date (LocalDate, format: yyyy-MM-dd).      |
| 02  | checkOut | Check-out date (LocalDate, format: yyyy-MM-dd).     |
| 03  | guests   | Number of guests (Integer, minimum: 1).             |
| 04  | priceMin | Minimum price per night filter (Integer, optional). |
| 05  | priceMax | Maximum price per night filter (Integer, optional). |

#### Room (DTO)

| No  | Method     | Description                                           |
| --- | ---------- | ----------------------------------------------------- |
| 01  | id         | Unique room identifier.                               |
| 02  | roomNumber | Room number displayed to users.                       |
| 03  | name       | Room name or title.                                   |
| 04  | type       | Bed layout type (e.g., "1 Giường Đôi Lớn").           |
| 05  | priceVnd   | Price per night in Vietnamese Dong.                   |
| 06  | capacity   | Maximum number of guests.                             |
| 07  | imageUrl   | Primary room image URL.                               |
| 08  | amenities  | Array of amenity names.                               |
| 09  | status     | Room status ("available", "occupied", "maintenance"). |
| 10  | isVisible  | Visibility flag for public display (Boolean).         |

#### PagedResponse<T>

| No  | Method     | Description                                      |
| --- | ---------- | ------------------------------------------------ |
| 01  | data       | List of room data (List&lt;T&gt;).               |
| 02  | total      | Total number of records matching criteria (int). |
| 03  | page       | Current page number (int).                       |
| 04  | pageSize   | Number of records per page (int).                |
| 05  | totalPages | Total number of pages (int).                     |

#### RoomController

| No  | Method              | Description                                                                                                                                                                                                                                                                                                                                                       |
| --- | ------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | roomService         | Dependency injection of RoomService interface.                                                                                                                                                                                                                                                                                                                    |
| 02  | checkAvailability() | **Input:** checkIn (String), checkOut (String), guests (Integer), priceMin (Integer, optional), priceMax (Integer, optional)<br>**Output:** ResponseEntity&lt;PagedResponse&lt;Room&gt;&gt;<br>**Processing:** Parses date strings to LocalDate, creates RoomAvailabilityRequest, calls roomService.checkAvailability(), returns available rooms with pagination. |

#### RoomService (Interface)

| No  | Method                 | Description                                                                                                                                                                                           |
| --- | ---------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | checkAvailability(req) | **Input:** req (RoomAvailabilityRequest)<br>**Output:** PagedResponse&lt;Room&gt;<br>**Processing:** Abstract method for finding available rooms based on date range, guest count, and price filters. |

#### RoomServiceImpl

| No  | Method                 | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| --- | ---------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | roomRepository         | Dependency injection of RoomRepository.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| 02  | checkAvailability(req) | **Input:** req (RoomAvailabilityRequest)<br>**Output:** PagedResponse&lt;Room&gt;<br>**Processing:** Validates checkIn and checkOut are not null, validates checkIn is before checkOut, validates checkIn is not in the past, sets default priceMin (0) and priceMax (999999999) if not provided, calls roomRepository.findAvailableRooms() with filters, filters results by isVisible=true, maps RoomEntity list to Room DTOs using RoomMapper, wraps results in PagedResponse (page=1, pageSize=all, total=count), returns response. |

#### RoomRepository (Interface)

| No  | Method               | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| --- | -------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | findAvailableRooms() | **Input:** minCapacity (Integer), checkIn (LocalDate), checkOut (LocalDate), minPrice (Integer), maxPrice (Integer)<br>**Output:** List&lt;RoomEntity&gt;<br>**Processing:** Custom JPQL query. Selects rooms with status='available', capacity >= minCapacity, price between minPrice and maxPrice. Uses NOT EXISTS subquery to exclude rooms with conflicting bookings (status IN ['pending', 'confirmed', 'checked_in']) where booking date range overlaps with search dates (checkIn < booking.checkOut AND checkOut > booking.checkIn). Returns list of available rooms ordered by price ascending. |

---

### c. Sequence Diagram

_[Provide the sequence diagram(s) for the feature, see the sample below]_

**![Sequence Diagram - Find Room](find_room_sequence.png)**

_Note: Export PlantUML diagram from `be_hotel/docs/diagrams/find_room_sequence.puml` và insert image vào đây_

**Flow Description:**

This sequence diagram shows the flow when a user searches for available rooms.

#### Main Flow:

1. **User Input:**

   - User opens homepage or search page
   - User fills in search form:
     - Check-in date (required)
     - Check-out date (required)
     - Number of guests (required)
     - Price range (optional)
   - User clicks "Tìm phòng" (Search) button

2. **API Request:**

   - Frontend validates form data (dates not empty, checkOut > checkIn)
   - Frontend sends GET request to `/api/rooms/availability?checkIn=2025-10-25&checkOut=2025-10-27&guests=2&priceMin=1000000&priceMax=5000000`
   - RoomController receives request (public endpoint, no auth required)

3. **Date Validation:**

   - Controller parses date strings to LocalDate format
   - Controller creates RoomAvailabilityRequest DTO
   - Controller calls RoomServiceImpl.checkAvailability(req)
   - Service validates:
     - checkIn is not null → else throw exception "Check-in date is required"
     - checkOut is not null → else throw exception "Check-out date is required"
     - checkIn < checkOut → else throw exception "Check-in must be before check-out"
     - checkIn >= today → else throw exception "Check-in date cannot be in the past"
   - If validation fails → return HTTP 400 Bad Request with error message

4. **Database Query:**

   - Service sets default values: priceMin = 0, priceMax = 999999999 (if not provided)
   - Service calls RoomRepository.findAvailableRooms(guests, checkIn, checkOut, priceMin, priceMax)
   - Repository executes JPQL query:
     - SELECT rooms WHERE status = 'available'
     - AND capacity >= guests
     - AND price BETWEEN priceMin AND priceMax
     - AND NOT EXISTS (conflicting bookings)
     - Conflicting booking logic: booking status IN ['pending', 'confirmed', 'checked_in'] AND (checkIn < booking.checkOut AND checkOut > booking.checkIn)
   - Repository returns List<RoomEntity>

5. **Filtering & Mapping:**

   - Service filters results: only rooms with isVisible = true
   - Service maps each RoomEntity to Room DTO using RoomMapper.toDto()
   - DTO includes: id, roomNumber, name, type, priceVnd, capacity, imageUrl, amenities, status

6. **Response:**
   - Service wraps results in PagedResponse (data, total, page=1, pageSize=all, totalPages=1)
   - Service returns PagedResponse to Controller
   - Controller returns HTTP 200 OK with JSON response
   - Frontend receives room list
   - Frontend displays results:
     - Shows total count: "Tìm thấy X phòng trống"
     - Renders room cards with image, name, price, capacity, amenities
     - Each card has "Xem chi tiết" button → navigate to /rooms/{id}
     - If no results → display "Không tìm thấy phòng phù hợp"

#### Error Handling Summary:

| Error Type               | HTTP Status | User Message                               |
| ------------------------ | ----------- | ------------------------------------------ |
| Missing checkIn/checkOut | 400         | "Vui lòng chọn ngày nhận/trả phòng"        |
| checkIn >= checkOut      | 400         | "Ngày trả phòng phải sau ngày nhận phòng"  |
| checkIn in past          | 400         | "Ngày nhận phòng không thể là quá khứ"     |
| Invalid date format      | 400         | "Định dạng ngày không hợp lệ"              |
| No rooms available       | 200         | "Không tìm thấy phòng trống" (empty array) |

---

### d. Database Queries

_[Provide the detailed SQL (select, insert, update...) which are used in implementing the function/screen]_

**Purpose:** Find available rooms based on date range, guest count, and price filters, excluding rooms with booking conflicts.

**SQL Queries:**

```sql
-- Query 1: Find available rooms (JPQL converted to SQL)
-- This is the core query for room availability checking
SELECT DISTINCT
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
    bl.layout_name AS type
FROM rooms r
LEFT JOIN bed_layouts bl ON r.bed_layout_id = bl.bed_layout_id
WHERE r.status = 'available'
  AND r.capacity >= 2  -- Parameter: @guests
  AND r.price_per_night BETWEEN 1000000 AND 5000000  -- Parameters: @priceMin, @priceMax
  AND r.is_visible = 1
  AND NOT EXISTS (
      -- Subquery: Check for conflicting bookings
      SELECT 1
      FROM bookings b
      WHERE b.room_id = r.room_id
        AND b.status IN ('pending', 'confirmed', 'checked_in')
        AND b.check_in < '2025-10-27'  -- Parameter: @checkOut
        AND b.check_out > '2025-10-25'  -- Parameter: @checkIn
  )
ORDER BY r.price_per_night ASC;

-- Query 2: Validate date range (application-level validation)
-- Check if checkIn is before checkOut
-- Check if checkIn is not in the past (compared to GETDATE())

-- Query 3: Count total available rooms (for pagination metadata)
SELECT COUNT(DISTINCT r.room_id)
FROM rooms r
WHERE r.status = 'available'
  AND r.capacity >= 2
  AND r.price_per_night BETWEEN 1000000 AND 5000000
  AND r.is_visible = 1
  AND NOT EXISTS (
      SELECT 1
      FROM bookings b
      WHERE b.room_id = r.room_id
        AND b.status IN ('pending', 'confirmed', 'checked_in')
        AND b.check_in < '2025-10-27'
        AND b.check_out > '2025-10-25'
  );
```

**Explanation:**

- **Query 1 (Find Available Rooms)**:

  - Selects rooms with status='available', sufficient capacity, within price range, and visible to public
  - Uses NOT EXISTS subquery to exclude rooms with booking conflicts
  - Conflict detection: checks if search date range overlaps with any active booking (pending, confirmed, checked_in)
  - Date overlap logic: `searchCheckIn < bookingCheckOut AND searchCheckOut > bookingCheckIn`
  - Orders results by price ascending (cheapest first)

- **Query 2 (Date Validation)**:

  - Performed in Java service layer
  - Ensures checkIn is before checkOut
  - Ensures checkIn is not in the past (compared to LocalDate.now())

- **Query 3 (Count Total)**:
  - Same filters as Query 1 but only counts results
  - Used for pagination metadata (total records, total pages)

**Result Example:**

| Query | Result                                                                  |
| ----- | ----------------------------------------------------------------------- |
| 1     | List of 5 available rooms: id=20, 21, 22, 24, 25 (room 23 has conflict) |
| 2     | Validation passed: checkIn=2025-10-25 < checkOut=2025-10-27             |
| 3     | total=5 available rooms matching criteria                               |

---

## Booking Conflict Detection Logic

**Date Overlap Algorithm:**

```
Search Range: [checkIn, checkOut)
Booking Range: [booking.checkIn, booking.checkOut)

Conflict exists if:
  checkIn < booking.checkOut AND checkOut > booking.checkIn

Example:
  Search: [2025-10-25, 2025-10-27)
  Booking: [2025-10-24, 2025-10-26)

  Check: 2025-10-25 < 2025-10-26 ✓ AND 2025-10-27 > 2025-10-24 ✓
  → CONFLICT! Room is not available
```

**Visual Representation:**

```
Case 1: Overlap (CONFLICT)
Search:    [====]
Booking: [======]

Case 2: Overlap (CONFLICT)
Search:  [======]
Booking:   [====]

Case 3: Overlap (CONFLICT)
Search:  [====]
Booking:     [====]

Case 4: No Overlap (AVAILABLE)
Search:  [====]
Booking:         [====]

Case 5: No Overlap (AVAILABLE)
Search:         [====]
Booking:  [====]
```

**Booking Status Considered:**

- ✅ **pending**: Reserved, awaiting payment
- ✅ **confirmed**: Paid, confirmed reservation
- ✅ **checked_in**: Guest is currently in the room
- ❌ **checked_out**: Guest has left, room is free
- ❌ **cancelled**: Booking was cancelled, room is free

---

## Frontend Integration

**API Call Example:**

```javascript
// fe_hotel/src/pages/Search.jsx
const searchAvailableRooms = async () => {
  const params = {
    checkIn: "2025-10-25",
    checkOut: "2025-10-27",
    guests: 2,
    priceMin: 1000000,
    priceMax: 5000000,
  };

  const response = await axios.get("/api/rooms/availability", { params });
  const rooms = response.data.data; // Extract from PagedResponse
  const total = response.data.total;

  setRooms(rooms);
  setTotalResults(total);
};
```

**Response Format:**

```json
{
  "data": [
    {
      "id": 20,
      "roomNumber": "101",
      "name": "Standard Twin",
      "type": "2 Giường Đơn",
      "priceVnd": 1500000,
      "capacity": 2,
      "imageUrl": "https://example.com/room101.jpg",
      "amenities": ["WiFi", "TV", "AC"],
      "status": "available",
      "isVisible": true
    }
  ],
  "total": 5,
  "page": 1,
  "pageSize": 5,
  "totalPages": 1
}
```

---

**End of Find Room Design Document**
