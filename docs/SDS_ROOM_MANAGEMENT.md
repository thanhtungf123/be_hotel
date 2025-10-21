# II. Code Designs - Room Management Feature

## 1. Manage Room (Admin) - Quản lý phòng

_[Provide the detailed design for the function <Feature/Function Name>. It include Class Diagram, Class Specifications, and Sequence Diagram(s)]_

---

### a. Class Diagram

_[This part presents the class diagram for the relevant feature]_

**![Class Diagram - Room Management](room_management_class_diagram.png)**

_Note: Export PlantUML diagram from `be_hotel/docs/diagrams/room_management_class_diagram.puml` và insert image vào đây_

---

### b. Class Specifications

_[Provide the description for each class and the methods in each class, following the table format as below]_

#### RoomRequest

| No  | Method        | Description                                                                    |
| --- | ------------- | ------------------------------------------------------------------------------ |
| 01  | roomNumber    | Room number (e.g., "101", "102"), must be unique.                              |
| 02  | roomName      | Room name or title (e.g., "Standard Twin").                                    |
| 03  | pricePerNight | Price per night in VND, must be positive integer.                              |
| 04  | description   | Detailed room description (optional).                                          |
| 05  | amenities     | Array of amenity names (e.g., ["WiFi", "TV"]).                                 |
| 06  | status        | Initial status: "available", "occupied", "maintenance" (default: "available"). |
| 07  | capacity      | Maximum number of guests.                                                      |
| 08  | imageUrl      | Primary room image URL.                                                        |
| 09  | bedLayoutId   | Foreign key to bed_layouts table.                                              |
| 10  | images        | Array of additional image URLs (optional).                                     |
| 11  | getters       | Standard getter methods.                                                       |
| 12  | setters       | Standard setter methods.                                                       |

#### Room

| No  | Method     | Description                           |
| --- | ---------- | ------------------------------------- |
| 01  | id         | Unique room identifier (primary key). |
| 02  | roomNumber | Room number displayed to users.       |
| 03  | name       | Room name or title.                   |
| 04  | priceVnd   | Price per night in Vietnamese Dong.   |
| 05  | status     | Current room status.                  |
| 06  | capacity   | Maximum number of guests.             |
| 07  | isVisible  | Visibility flag (default: true).      |
| 08  | imageUrl   | Primary image URL.                    |
| 09  | type       | Bed layout type.                      |
| 10  | amenities  | Array of amenity names.               |
| 11  | getters    | Standard getter methods.              |
| 12  | setters    | Standard setter methods.              |

#### RoomController

| No  | Method                  | Description                                                                                                                                                                                                                                                                                                   |
| --- | ----------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | roomService             | Dependency injection of RoomService interface.                                                                                                                                                                                                                                                                |
| 02  | authHelper              | Dependency injection of AuthorizationHelper.                                                                                                                                                                                                                                                                  |
| 03  | createRoom(request)     | **Input:** RoomRequest (room details)<br>**Output:** ResponseEntity&lt;Room&gt; - HTTP 201 Created with new room JSON<br>**Processing:** Validates admin authorization, checks room number uniqueness, calls roomService.createRoom(), returns created room.                                                  |
| 04  | updateRoom(id, request) | **Input:** id (Long), RoomRequest (updated details)<br>**Output:** ResponseEntity&lt;Room&gt; - HTTP 200 OK with updated room JSON<br>**Processing:** Validates admin authorization, checks room exists, validates room number uniqueness (if changed), calls roomService.updateRoom(), returns updated room. |
| 05  | deleteRoom(id)          | **Input:** id (Long, room identifier)<br>**Output:** ResponseEntity&lt;Void&gt; - HTTP 204 No Content<br>**Processing:** Validates admin authorization, checks room exists, calls roomService.deleteRoom(), returns success response.                                                                         |

#### RoomService (Interface)

| No  | Method                  | Description                                                                                                                              |
| --- | ----------------------- | ---------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | createRoom(request)     | **Input:** RoomRequest<br>**Output:** Room DTO<br>**Processing:** Abstract method for creating new room with validation.                 |
| 02  | updateRoom(id, request) | **Input:** id (Long), RoomRequest<br>**Output:** Room DTO<br>**Processing:** Abstract method for updating existing room with validation. |
| 03  | deleteRoom(id)          | **Input:** id (Long)<br>**Output:** void<br>**Processing:** Abstract method for deleting room.                                           |

#### RoomServiceImpl

| No  | Method                  | Description                                                                                                                                                                                                                                                                                                                                                                   |
| --- | ----------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | roomRepository          | Dependency injection of RoomRepository.                                                                                                                                                                                                                                                                                                                                       |
| 02  | bedLayoutRepository     | Dependency injection of BedLayoutRepository.                                                                                                                                                                                                                                                                                                                                  |
| 03  | createRoom(request)     | **Input:** RoomRequest<br>**Output:** Room DTO<br>**Processing:** Validates room number uniqueness via roomRepository.existsByRoomNumber(), validates price > 0, validates bed layout exists, creates new RoomEntity, sets default isVisible=true, saves to database via repository.save(), maps entity to DTO and returns.                                                   |
| 04  | updateRoom(id, request) | **Input:** id (Long), RoomRequest<br>**Output:** Room DTO<br>**Processing:** Fetches existing room via findById(), throws exception if not found, validates room number uniqueness if changed (excluding current room), validates price > 0, validates bed layout if changed, updates entity fields, saves to database via repository.save(), maps entity to DTO and returns. |
| 05  | deleteRoom(id)          | **Input:** id (Long)<br>**Output:** void<br>**Processing:** Fetches room via findById(), throws exception if not found, calls repository.deleteById(id) to remove from database. Note: Should check for active bookings before deletion in production.                                                                                                                        |

#### RoomRepository (Interface)

| No  | Method                     | Description                                                                                                                                                     |
| --- | -------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | save(room)                 | **Input:** RoomEntity<br>**Output:** RoomEntity (with generated ID)<br>**Processing:** JPA method. Inserts new room or updates existing room.                   |
| 02  | findById(id)               | **Input:** id (Long)<br>**Output:** Optional&lt;RoomEntity&gt;<br>**Processing:** JPA method. Fetches room by ID.                                               |
| 03  | deleteById(id)             | **Input:** id (Long)<br>**Output:** void<br>**Processing:** JPA method. Deletes room by ID.                                                                     |
| 04  | existsByRoomNumber(number) | **Input:** roomNumber (String)<br>**Output:** boolean<br>**Processing:** Custom query method. Checks if room number already exists (for uniqueness validation). |

---

### c. Sequence Diagram

_[Provide the sequence diagram(s) for the feature, see the sample below]_

**![Sequence Diagram - Room Management CRUD](room_management_sequence.png)**

_Note: Export PlantUML diagram from `be_hotel/docs/diagrams/room_management_sequence.puml` và insert image vào đây_

**Flow Description:**

This sequence diagram covers Create, Update, and Delete operations for room management.

#### Main Flow:

**1. CREATE ROOM (POST /api/rooms)**

- Admin opens "Thêm phòng mới" form in admin panel
- Admin fills in room details: room number, name, price, capacity, bed layout, amenities, image URL
- Frontend validates required fields client-side
- Frontend retrieves X-Auth-Token from localStorage
- Frontend sends POST request to `/api/rooms` with RoomRequest body
- RoomController validates admin authorization via authHelper.requireAdmin()
- If not admin → returns HTTP 403 Forbidden
- Controller calls RoomServiceImpl.createRoom(request)
- Service validates room number uniqueness via roomRepository.existsByRoomNumber()
- If duplicate → throws IllegalArgumentException "Room number already exists"
- Service validates price > 0
- If invalid → throws IllegalArgumentException "Price must be positive"
- Service validates bed layout exists via bedLayoutRepository.findById()
- If not found → throws IllegalArgumentException "Bed layout not found"
- Service creates new RoomEntity with default isVisible=true, status="available"
- Service calls roomRepository.save(entity)
- Database executes INSERT INTO rooms
- Repository returns saved entity with generated ID
- Service maps entity to Room DTO
- Controller returns HTTP 201 Created with room JSON
- Frontend displays success message and refreshes table

**2. UPDATE ROOM (PUT /api/rooms/{id})**

- Admin clicks "Sửa" button for existing room
- Frontend loads room data into edit form
- Admin modifies fields (e.g., price, amenities, status)
- Frontend sends PUT request to `/api/rooms/{id}` with updated RoomRequest
- Controller validates admin authorization
- Controller calls RoomServiceImpl.updateRoom(id, request)
- Service fetches existing room via roomRepository.findById(id)
- If not found → throws IllegalArgumentException "Room not found"
- Service validates room number uniqueness if changed (exclude current room)
- Service validates price > 0
- Service validates bed layout if bedLayoutId changed
- Service updates entity fields with new values
- Service calls roomRepository.save(entity)
- Database executes UPDATE rooms SET ... WHERE room_id = ?
- Repository returns updated entity
- Service maps entity to Room DTO
- Controller returns HTTP 200 OK with updated room JSON
- Frontend displays success message and refreshes table

**3. DELETE ROOM (DELETE /api/rooms/{id})**

- Admin clicks "Xóa" button for room
- Frontend confirms via window.confirm() "Bạn có chắc muốn xóa phòng này?"
- If cancelled → no action
- If confirmed → Frontend sends DELETE request to `/api/rooms/{id}`
- Controller validates admin authorization
- Controller calls RoomServiceImpl.deleteRoom(id)
- Service fetches room via roomRepository.findById(id)
- If not found → throws IllegalArgumentException "Room not found"
- Service calls roomRepository.deleteById(id)
- Database executes DELETE FROM rooms WHERE room_id = ?
- Controller returns HTTP 204 No Content
- Frontend removes room row from table
- Frontend displays success message "Xóa phòng thành công!"

#### Error Handling Summary:

| Error Type                 | HTTP Status     | User Message                |
| -------------------------- | --------------- | --------------------------- |
| Not Admin                  | 403 Forbidden   | "Không có quyền truy cập"   |
| Room Number Already Exists | 400 Bad Request | "Số phòng đã tồn tại"       |
| Price Invalid              | 400 Bad Request | "Giá phòng phải lớn hơn 0"  |
| Bed Layout Not Found       | 400 Bad Request | "Loại giường không tồn tại" |
| Room Not Found             | 404 Not Found   | "Không tìm thấy phòng"      |

---

### d. Database Queries

_[Provide the detailed SQL (select, insert, update...) which are used in implementing the function/screen]_

**Purpose:** Database queries used in room CRUD operations including validation, insert, update, and delete.

**SQL Queries:**

```sql
-- Query 1: Check room number uniqueness (Validation)
SELECT COUNT(*) AS exists
FROM rooms
WHERE room_number = '101';  -- Thay '101' bằng room number cụ thể

-- Query 2: Insert new room (CREATE)
INSERT INTO rooms (
    room_number,
    room_name,
    price_per_night,
    description,
    amenities,
    status,
    capacity,
    image_url,
    bed_layout_id,
    is_visible
)
VALUES (
    '101',           -- room_number
    'Standard Twin', -- room_name
    1500000,         -- price_per_night (VND)
    'Phòng tiêu chuẩn với 2 giường đơn',  -- description
    'WiFi,TV,Air Conditioning',  -- amenities (comma-separated)
    'available',     -- status
    2,               -- capacity
    'https://example.com/room101.jpg',  -- image_url
    1,               -- bed_layout_id
    1                -- is_visible (default true)
);

-- Query 3: Get room by ID for update/delete
SELECT
    r.room_id,
    r.room_number,
    r.room_name,
    r.price_per_night,
    r.description,
    r.amenities,
    r.status,
    r.capacity,
    r.image_url,
    r.bed_layout_id,
    r.is_visible
FROM rooms r
WHERE r.room_id = 20;  -- Thay 20 bằng room ID cụ thể

-- Query 4: Update room (UPDATE)
UPDATE rooms
SET
    room_number = '101',       -- Có thể thay đổi
    room_name = 'Deluxe Twin', -- Updated name
    price_per_night = 2000000, -- Updated price
    description = 'Phòng cao cấp...',
    amenities = 'WiFi,TV,Mini Bar',
    status = 'available',
    capacity = 2,
    image_url = 'https://example.com/new-image.jpg',
    bed_layout_id = 1
WHERE room_id = 20;  -- Thay 20 bằng room ID cụ thể

-- Query 5: Delete room (DELETE)
DELETE FROM rooms
WHERE room_id = 20;  -- Thay 20 bằng room ID cụ thể

-- Query 6: Check bed layout exists (Validation)
SELECT COUNT(*) AS exists
FROM bed_layouts
WHERE bed_layout_id = 1;  -- Thay 1 bằng bed_layout_id cụ thể

-- Query 7: Get all rooms for admin table (LIST)
SELECT
    r.room_id,
    r.room_number,
    r.room_name,
    r.price_per_night,
    r.status,
    r.capacity,
    r.is_visible,
    bl.layout_name AS type
FROM rooms r
LEFT JOIN bed_layouts bl ON r.bed_layout_id = bl.bed_layout_id
ORDER BY r.room_id ASC;
```

**Explanation:**

- **Query 1 (Validation)**: Checks if room number already exists before CREATE or UPDATE
- **Query 2 (CREATE)**: Inserts new room with all fields, sets default is_visible=1 and status='available'
- **Query 3 (READ)**: Fetches room details by ID for UPDATE or DELETE operations
- **Query 4 (UPDATE)**: Updates all room fields for existing room
- **Query 5 (DELETE)**: Removes room from database (should check for active bookings first in production)
- **Query 6 (Validation)**: Ensures bed layout ID is valid before CREATE/UPDATE
- **Query 7 (LIST)**: Retrieves all rooms for admin management table with bed layout info

**Result Example:**

| Query | Result                                               |
| ----- | ---------------------------------------------------- |
| 1     | exists=0 (unique) or exists=1 (duplicate)            |
| 2     | (1 row inserted, returns generated room_id)          |
| 3     | Room data: id=20, number="101", name="Standard Twin" |
| 4     | (1 row affected)                                     |
| 5     | (1 row deleted)                                      |
| 6     | exists=1 (valid) or exists=0 (invalid)               |
| 7     | List of all rooms with id, number, name, price, type |

---

## Validation Rules Summary

**Business Rules:**

| Field         | Validation Rule                                   | Error Message               |
| ------------- | ------------------------------------------------- | --------------------------- |
| roomNumber    | Must be unique                                    | "Số phòng đã tồn tại"       |
| roomNumber    | Required, not empty                               | "Số phòng là bắt buộc"      |
| roomName      | Required, not empty                               | "Tên phòng là bắt buộc"     |
| pricePerNight | Must be > 0                                       | "Giá phòng phải lớn hơn 0"  |
| capacity      | Must be > 0                                       | "Sức chứa phải lớn hơn 0"   |
| bedLayoutId   | Must exist in bed_layouts table                   | "Loại giường không tồn tại" |
| status        | Must be in ['available','occupied','maintenance'] | "Trạng thái không hợp lệ"   |

**Default Values:**

- `isVisible`: true (1) - room is visible by default
- `status`: "available" - room is available for booking by default

---

**End of Room Management Design Document**
