# II. Code Designs - View Room Feature

## 1. View Room (Xem danh sách phòng với filter)

_[Provide the detailed design for the function <Feature/Function Name>. It include Class Diagram, Class Specifications, and Sequence Diagram(s)]_

---

### a. Class Diagram

_[This part presents the class diagram for the relevant feature]_

**![Class Diagram - View Room](view_room_class_diagram.png)**

_Note: Export PlantUML diagram from `be_hotel/docs/diagrams/view_room_class_diagram.puml` và insert image vào đây_

---

### b. Class Specifications

_[Provide the description for each class and the methods in each class, following the table format as below]_

#### RoomSearchCriteria

| No  | Method    | Description                                                  |
| --- | --------- | ------------------------------------------------------------ |
| 01  | types     | List of bed layout types to filter (e.g., ["1 Giường Đôi"]). |
| 02  | amenities | List of amenity names to filter (e.g., ["WiFi", "Pool"]).    |
| 03  | priceMin  | Minimum price per night (Integer, optional, default: null).  |
| 04  | priceMax  | Maximum price per night (Integer, optional, default: null).  |
| 05  | status    | List of room statuses (e.g., ["available", "occupied"]).     |
| 06  | sort      | Sort field and direction (e.g., "price:asc", "name:desc").   |
| 07  | page      | Page number for pagination (Integer, default: 1).            |
| 08  | pageSize  | Number of records per page (Integer, default: 10).           |

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

| No  | Method      | Description                                                                                                                                                                                                                                                                                                                                                                                                                                        |
| --- | ----------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | roomService | Dependency injection of RoomService interface.                                                                                                                                                                                                                                                                                                                                                                                                     |
| 02  | search()    | **Input:** types (String, comma-separated), amenities (String, comma-separated), priceMin (Integer), priceMax (Integer), status (String, comma-separated), sort (String), page (Integer), pageSize (Integer)<br>**Output:** ResponseEntity&lt;PagedResponse&lt;Room&gt;&gt;<br>**Processing:** Parses comma-separated strings to lists, creates RoomSearchCriteria, calls roomService.search(), returns filtered and sorted rooms with pagination. |

#### RoomService (Interface)

| No  | Method           | Description                                                                                                                                                                                                    |
| --- | ---------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | search(criteria) | **Input:** criteria (RoomSearchCriteria)<br>**Output:** PagedResponse&lt;Room&gt;<br>**Processing:** Abstract method for searching and filtering rooms based on multiple criteria with sorting and pagination. |

#### RoomServiceImpl

| No  | Method           | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| --- | ---------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | roomRepository   | Dependency injection of RoomRepository.                                                                                                                                                                                                                                                                                                                                                                                                                                             |
| 02  | search(criteria) | **Input:** criteria (RoomSearchCriteria)<br>**Output:** PagedResponse&lt;Room&gt;<br>**Processing:** Sets default priceMin (0) and priceMax (999999999) if not provided, parses sort string to field and direction (default: "price:asc"), calls roomRepository.findForList() with filters, sort, and pagination, filters results by isVisible=true, maps RoomEntity list to Room DTOs using RoomMapper, wraps results in PagedResponse with pagination metadata, returns response. |

#### RoomRepository (Interface)

| No  | Method        | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| --- | ------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | findForList() | **Input:** statusList (List&lt;String&gt;), typeList (List&lt;String&gt;), priceMin (Integer), priceMax (Integer), amenitiesFilter (String, comma-separated), pageable (Pageable with sort)<br>**Output:** Page&lt;RoomEntity&gt;<br>**Processing:** Custom JPQL query. Selects rooms with status IN statusList, type IN typeList (if provided), price BETWEEN priceMin AND priceMax. If amenitiesFilter is provided, uses LIKE '%amenity%' for each amenity. Applies sorting and pagination from Pageable, returns paginated results. |

---

### c. Sequence Diagram

_[Provide the sequence diagram(s) for the feature, see the sample below]_

**![Sequence Diagram - View Room](view_room_sequence.png)**

_Note: Export PlantUML diagram from `be_hotel/docs/diagrams/view_room_sequence.puml` và insert image vào đây_

**Flow Description:**

This sequence diagram shows the flow when a user views and filters the room list.

#### Main Flow:

1. **User Navigation:**

   - User navigates to Search page (/search)
   - Page loads with FilterSidebar (left) and RoomList (right)
   - FilterSidebar displays filter options:
     - Price range slider (priceMin - priceMax)
     - Room types checkboxes (1 Giường Đôi Lớn, 2 Giường Đơn, etc.)
     - Amenities checkboxes (WiFi, Pool, Gym, etc.)
     - Status checkboxes (available, occupied, maintenance)

2. **Initial Load:**

   - Frontend sends GET request to `/api/rooms/search` (no filters)
   - Default behavior: show all "available" and "visible" rooms
   - Sorted by price ascending

3. **User Applies Filters:**

   - User selects filters:
     - Types: ["1 Giường Đôi Lớn", "2 Giường Đơn"]
     - Amenities: ["WiFi", "Pool"]
     - Price range: 1,000,000 - 5,000,000 VND
     - Status: ["available"]
   - User clicks "Áp dụng" or filter auto-applies
   - Frontend updates URL query params

4. **API Request:**

   - Frontend sends GET request:
     `/api/rooms/search?types=1%20Giường%20Đôi%20Lớn,2%20Giường%20Đơn&amenities=WiFi,Pool&priceMin=1000000&priceMax=5000000&status=available&sort=price:asc&page=1&pageSize=10`
   - RoomController receives request (public endpoint, no auth required)

5. **Parse Parameters:**

   - Controller parses comma-separated strings:
     - types: "1 Giường Đôi Lớn,2 Giường Đơn" → ["1 Giường Đôi Lớn", "2 Giường Đơn"]
     - amenities: "WiFi,Pool" → ["WiFi", "Pool"]
     - status: "available" → ["available"]
   - Controller creates RoomSearchCriteria DTO
   - Controller calls RoomServiceImpl.search(criteria)

6. **Database Query:**

   - Service sets default values if missing
   - Service parses sort string: "price:asc" → field="price", direction="asc"
   - Service creates Pageable with sorting
   - Service calls RoomRepository.findForList(statusList, typeList, priceMin, priceMax, amenities, pageable)
   - Repository executes JPQL query:
     - WHERE status IN statusList
     - AND type IN typeList (if provided)
     - AND price BETWEEN priceMin AND priceMax
     - AND amenities LIKE '%WiFi%' AND amenities LIKE '%Pool%' (if provided)
     - ORDER BY price ASC
     - LIMIT 10 OFFSET 0 (pagination)
   - Repository returns Page<RoomEntity>

7. **Filtering & Mapping:**

   - Service filters results: only rooms with isVisible = true
   - Service maps each RoomEntity to Room DTO using RoomMapper.toDto()
   - DTO includes: id, roomNumber, name, type, priceVnd, capacity, imageUrl, amenities, status

8. **Response:**

   - Service wraps results in PagedResponse (data, total, page, pageSize, totalPages)
   - Service returns PagedResponse to Controller
   - Controller returns HTTP 200 OK with JSON response

9. **UI Rendering:**

   - Frontend receives room list
   - Frontend displays results:
     - SortBar: "Tìm thấy X phòng" + sort dropdown
     - RoomCardRow for each room:
       - Image, name, type, capacity
       - Price per night
       - Amenities badges
       - Status badge
       - "Xem chi tiết" button
     - Pagination controls (if totalPages > 1)
     - If no results → display "Không tìm thấy phòng phù hợp. Thử điều chỉnh bộ lọc."

10. **User Changes Sort:**

    - User selects "Giá: Cao → Thấp" in sort dropdown
    - Frontend updates sort param to "price:desc"
    - Re-sends API request with new sort
    - Backend re-queries with ORDER BY price DESC
    - Frontend updates room list order

11. **User Clears Filters:**
    - User clicks "Xóa bộ lọc" button
    - Frontend resets all filters to default
    - Re-sends API request without filter params
    - Shows all available rooms

#### Error Handling Summary:

| Error Type          | HTTP Status | User Message                         |
| ------------------- | ----------- | ------------------------------------ |
| Invalid sort format | 400         | "Định dạng sắp xếp không hợp lệ"     |
| Invalid page number | 400         | "Số trang không hợp lệ"              |
| No rooms found      | 200         | "Không tìm thấy phòng" (empty array) |

---

### d. Database Queries

_[Provide the detailed SQL (select, insert, update...) which are used in implementing the function/screen]_

**Purpose:** Search and filter rooms with multiple criteria, sorting, and pagination.

**SQL Queries:**

```sql
-- Query 1: Search rooms with filters (JPQL converted to SQL)
-- This is the main query for room listing with filters
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
WHERE r.status IN ('available')  -- Parameter: @statusList
  AND bl.layout_name IN ('1 Giường Đôi Lớn', '2 Giường Đơn')  -- Parameter: @typeList (optional)
  AND r.price_per_night BETWEEN 1000000 AND 5000000  -- Parameters: @priceMin, @priceMax
  AND r.amenities LIKE '%WiFi%'  -- Parameter: @amenities (applied per amenity)
  AND r.amenities LIKE '%Pool%'
  AND r.is_visible = 1
ORDER BY r.price_per_night ASC  -- Parameter: @sort (field and direction)
OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY;  -- Parameters: @page, @pageSize

-- Query 2: Count total matching rooms (for pagination metadata)
SELECT COUNT(DISTINCT r.room_id)
FROM rooms r
LEFT JOIN bed_layouts bl ON r.bed_layout_id = bl.bed_layout_id
WHERE r.status IN ('available')
  AND bl.layout_name IN ('1 Giường Đôi Lớn', '2 Giường Đơn')
  AND r.price_per_night BETWEEN 1000000 AND 5000000
  AND r.amenities LIKE '%WiFi%'
  AND r.amenities LIKE '%Pool%'
  AND r.is_visible = 1;

-- Query 3: Get available bed layout types (for filter options)
SELECT DISTINCT bl.layout_name
FROM bed_layouts bl
ORDER BY bl.layout_name;

-- Query 4: Get all unique amenities (for filter options)
-- This is done programmatically by parsing the amenities field from all rooms
SELECT DISTINCT r.amenities
FROM rooms r
WHERE r.is_visible = 1;
```

**Explanation:**

- **Query 1 (Search with Filters)**:

  - Filters by status, bed layout type, price range, and amenities
  - Uses LIKE '%amenity%' for each amenity in the filter (AND logic)
  - Only shows visible rooms (isVisible = 1)
  - Supports dynamic sorting (price, name, capacity)
  - Implements pagination using OFFSET and FETCH NEXT

- **Query 2 (Count Total)**:

  - Same filters as Query 1 but only counts results
  - Used for pagination metadata (total records, total pages)

- **Query 3 (Get Bed Layout Types)**:

  - Fetches distinct bed layout types for filter dropdown
  - Used to populate "Loại phòng" checkboxes

- **Query 4 (Get Amenities)**:
  - Extracts unique amenities from all rooms
  - Amenities are stored as comma-separated strings
  - Parsed programmatically to create amenity list for filter

**Result Example:**

| Query | Result                                                               |
| ----- | -------------------------------------------------------------------- |
| 1     | Page 1 of 10 rooms: id=20, 21, 22, 24, 25, 26, 27, 28, 29, 30        |
| 2     | total=25 rooms matching all filters                                  |
| 3     | ["1 Giường Đơn", "1 Giường Đôi Lớn", "2 Giường Đơn", "2 Giường Đôi"] |
| 4     | ["WiFi", "TV", "AC", "Mini Bar", "Pool", "Gym", "Spa"]               |

---

## Filter & Sort Options

### 1. Price Range Filter

```javascript
// Frontend slider range
priceMin: 0 (min)
priceMax: 10,000,000 VND (max)

// User selects: 1,000,000 - 5,000,000
// API: ?priceMin=1000000&priceMax=5000000
```

### 2. Room Type Filter

```javascript
// Checkboxes for bed layouts
types: ["1 Giường Đơn", "1 Giường Đôi Lớn", "2 Giường Đơn", "2 Giường Đôi"];

// User selects: 1 Giường Đôi Lớn, 2 Giường Đơn
// API: ?types=1%20Giường%20Đôi%20Lớn,2%20Giường%20Đơn
```

### 3. Amenities Filter

```javascript
// Checkboxes for amenities
amenities: [
  "WiFi miễn phí",
  "TV màn hình phẳng",
  "Điều hòa không khí",
  "Mini Bar",
  "Hồ bơi",
  "Phòng gym",
  "Spa",
];

// User selects: WiFi, Pool
// API: ?amenities=WiFi,Pool
// SQL: WHERE amenities LIKE '%WiFi%' AND amenities LIKE '%Pool%'
```

### 4. Status Filter

```javascript
// Checkboxes for room status
status: [
  "available" → "Có sẵn",
  "occupied" → "Đã đặt",
  "maintenance" → "Bảo trì"
]

// User selects: available
// API: ?status=available
```

### 5. Sort Options

```javascript
// Dropdown for sorting
sortOptions: [
  "price:asc" → "Giá: Thấp → Cao",
  "price:desc" → "Giá: Cao → Thấp",
  "name:asc" → "Tên: A → Z",
  "name:desc" → "Tên: Z → A",
  "capacity:desc" → "Sức chứa: Cao → Thấp"
]

// User selects: Giá: Thấp → Cao
// API: ?sort=price:asc
// SQL: ORDER BY price_per_night ASC
```

### 6. Pagination

```javascript
// Pagination controls
page: 1 (current page)
pageSize: 10 (items per page)
totalPages: 3 (calculated from total/pageSize)

// User clicks page 2
// API: ?page=2&pageSize=10
// SQL: OFFSET 10 ROWS FETCH NEXT 10 ROWS ONLY
```

---

## Frontend Components

### FilterSidebar.jsx

```javascript
// State for filters
const [filters, setFilters] = useState({
  types: [],
  amenities: [],
  priceMin: 0,
  priceMax: 10000000,
  status: ["available"],
});

// Apply filters
const handleApplyFilters = () => {
  const params = {
    types: filters.types.join(","),
    amenities: filters.amenities.join(","),
    priceMin: filters.priceMin,
    priceMax: filters.priceMax,
    status: filters.status.join(","),
  };
  // Call API with params
};
```

### SortBar.jsx

```javascript
// Sort dropdown
const [sort, setSort] = useState("price:asc");

const handleSortChange = (newSort) => {
  setSort(newSort);
  // Re-fetch data with new sort
};
```

### RoomCardRow.jsx

```javascript
// Display each room in a card
{
  rooms.map((room) => (
    <Card key={room.id}>
      <Image src={room.imageUrl} />
      <Card.Body>
        <h5>{room.name}</h5>
        <p>
          {room.type} • {room.capacity} khách
        </p>
        <h4>{room.priceVnd.toLocaleString()} VND/đêm</h4>
        <Badges amenities={room.amenities} />
        <Button onClick={() => navigate(`/rooms/${room.id}`)}>
          Xem chi tiết
        </Button>
      </Card.Body>
    </Card>
  ));
}
```

---

**End of View Room Design Document**
