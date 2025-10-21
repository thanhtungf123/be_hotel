# II. Code Designs - Show/Hide Room Feature

## 1. Show/Hide Room (Hiển thị/Ẩn phòng)

_[Provide the detailed design for the function <Feature/Function Name>. It include Class Diagram, Class Specifications, and Sequence Diagram(s)]_

---

### a. Class Diagram

_[This part presents the class diagram for the relevant feature]_

**![Class Diagram - Show/Hide Room](show_hide_room_class_diagram.png)**

_Note: Export PlantUML diagram from `be_hotel/docs/diagrams/show_hide_room_class_diagram.puml` và insert image vào đây_

---

### b. Class Specifications

_[Provide the description for each class and the methods in each class, following the table format as below]_

#### Room

| No  | Method     | Description                                                                       |
| --- | ---------- | --------------------------------------------------------------------------------- |
| 01  | id         | Unique room identifier (primary key).                                             |
| 02  | roomNumber | Room number displayed to users (e.g., "101", "102", "201").                       |
| 03  | name       | Room name or title (e.g., "Standard Twin", "Deluxe Ocean View").                  |
| 04  | isVisible  | Visibility flag: true = shown in search results, false = hidden from public view. |
| 05  | status     | Current room status: "available", "occupied", "maintenance".                      |
| 06  | priceVnd   | Price per night in Vietnamese Dong (VND).                                         |
| 07  | getters    | Standard getter methods for all fields.                                           |
| 08  | setters    | Standard setter methods for all fields.                                           |

#### RoomController

| No  | Method                     | Description                                                                                                                                                                                                                                                                                                                         |
| --- | -------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | roomService                | Dependency injection of RoomService interface for business logic processing.                                                                                                                                                                                                                                                        |
| 02  | authHelper                 | Dependency injection of AuthorizationHelper for admin authorization checking.                                                                                                                                                                                                                                                       |
| 03  | toggleVisibility(id, body) | **Input:** id (Long, room identifier), body (Map with "isVisible" boolean)<br>**Output:** ResponseEntity&lt;Room&gt; - JSON response with updated room<br>**Processing:** Validates admin authorization, extracts isVisible from request body, calls roomService.toggleVisibility(), returns updated room as JSON with HTTP 200 OK. |

#### RoomService (Interface)

| No  | Method                          | Description                                                                                                                                                                                                             |
| --- | ------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | toggleVisibility(id, isVisible) | **Input:** id (Long, room identifier), isVisible (Boolean)<br>**Output:** void<br>**Processing:** Abstract method defining room visibility toggle contract to be implemented by service layer with transaction support. |

#### RoomServiceImpl

| No  | Method                          | Description                                                                                                                                                                                                                                                                                                                                                                     |
| --- | ------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | roomRepository                  | Dependency injection of RoomRepository for room data access.                                                                                                                                                                                                                                                                                                                    |
| 02  | toggleVisibility(id, isVisible) | **Input:** id (Long), isVisible (Boolean)<br>**Output:** void<br>**Processing:** Main visibility toggle handler with @Transactional annotation. Fetches RoomEntity from repository via findById(), throws exception if room not found, updates room.isVisible field with new value, saves updated entity to database via repository.save(). No business rule validation needed. |

#### RoomRepository (Interface)

| No  | Method       | Description                                                                                                                                                                                                                                                            |
| --- | ------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | findById(id) | **Input:** id (Long, room identifier)<br>**Output:** Optional&lt;RoomEntity&gt;<br>**Processing:** JPA built-in method. Fetches single room entity by primary key. Returns Optional.empty() if not found.                                                              |
| 02  | save(room)   | **Input:** room (RoomEntity with updated isVisible field)<br>**Output:** RoomEntity (saved entity with updated timestamp)<br>**Processing:** JPA built-in method. Persists room entity changes to database. Used within @Transactional context for visibility updates. |

#### RoomEntity

| No  | Method     | Description                                                                              |
| --- | ---------- | ---------------------------------------------------------------------------------------- |
| 01  | id         | Primary key (room_id in database).                                                       |
| 02  | roomNumber | Room number string.                                                                      |
| 03  | roomName   | Full room name.                                                                          |
| 04  | isVisible  | Boolean flag for visibility (mapped to is_visible BIT column in database, default true). |
| 05  | status     | Room status string.                                                                      |
| 06  | getters    | Standard getter methods.                                                                 |
| 07  | setters    | Standard setter methods.                                                                 |

---

### c. Sequence Diagram

_[Provide the sequence diagram(s) for the feature, see the sample below]_

**![Sequence Diagram - Show/Hide Room Complete Flow](show_hide_room_sequence.png)**

_Note: Export PlantUML diagram from `be_hotel/docs/diagrams/show_hide_room_sequence.puml` và insert image vào đây_

**Flow Description:**

This sequence diagram covers both show and hide operations with success and error handling.

#### Main Flow:

1. **Initiation:**

   - Admin views room management table
   - Admin clicks visibility toggle button (Hiển thị/Ẩn) for a specific room
   - Frontend detects current visibility state: if visible → will hide (isVisible=false), if hidden → will show (isVisible=true)
   - Frontend confirms action via window.confirm() dialog

2. **Authorization:**

   - Frontend retrieves X-Auth-Token from localStorage
   - Sends PATCH request to `/api/rooms/{id}/visibility` with body: `{"isVisible": true/false}`
   - RoomController validates admin authorization via authHelper.requireAdmin()
   - If not admin → returns HTTP 403 Forbidden (flow ends)

3. **Processing:**

   - Controller extracts id (Long) and isVisible (Boolean) from request
   - Controller calls RoomServiceImpl.toggleVisibility(id, isVisible)
   - Service fetches room from RoomRepository.findById(id)
   - If room not found → throws exception, returns HTTP 404 Not Found
   - If room found → Service updates room.isVisible = newValue

4. **Database Update:**

   - Service calls RoomRepository.save(room) with @Transactional context
   - Database executes: `UPDATE rooms SET is_visible = ? WHERE room_id = ?`
   - Repository returns updated RoomEntity

5. **Success Response:**

   - Controller maps updated RoomEntity to Room DTO
   - Returns HTTP 200 OK with updated room JSON
   - Frontend receives response and updates UI:
     - Toggle button label changes: "Hiển thị" ↔ "Ẩn"
     - Badge color changes: "Hiển thị" (success/green) ↔ "Ẩn" (secondary/gray)
   - Admin sees confirmation alert: "Cập nhật hiển thị thành công!"

6. **Side Effects:**

   - If isVisible = false (hidden):
     - Room disappears from public search results (/api/rooms/search)
     - Room disappears from availability checks (/api/rooms/availability)
     - Room still visible in admin management table (/api/rooms/admin/all)
   - If isVisible = true (shown):
     - Room reappears in public search results (if status = 'available')
     - Room reappears in availability checks
   - Note: Rooms with status='maintenance' are automatically hidden regardless of isVisible flag

#### Error Handling Summary:

| Error Type     | HTTP Status   | User Message              |
| -------------- | ------------- | ------------------------- |
| Not Admin      | 403 Forbidden | "Không có quyền truy cập" |
| Room Not Found | 404 Not Found | "Không tìm thấy phòng"    |

---

### d. Database Queries

_[Provide the detailed SQL (select, insert, update...) which are used in implementing the function/screen]_

**Purpose:** Database queries used in show/hide room workflow, including read and update operations.

**SQL Queries:**

```sql
-- Query 1: Lấy thông tin phòng hiện tại
SELECT
    r.room_id,
    r.room_number,
    r.room_name,
    r.is_visible,
    r.status,
    r.price_per_night
FROM rooms r
WHERE r.room_id = 101;  -- Thay 101 bằng room ID cụ thể

-- Query 2: Cập nhật visibility của phòng
UPDATE rooms
SET is_visible = 0  -- 0 = ẩn (false), 1 = hiển thị (true)
WHERE room_id = 101;  -- Thay 101 bằng room ID cụ thể

-- Query 3: Lấy danh sách phòng cho public search (chỉ visible rooms)
SELECT
    r.room_id,
    r.room_number,
    r.room_name,
    r.status,
    r.price_per_night,
    bl.layout_name
FROM rooms r
LEFT JOIN bed_layouts bl ON r.bed_layout_id = bl.bed_layout_id
WHERE r.status = 'available'
  AND r.is_visible = 1  -- Chỉ lấy phòng đang hiển thị
ORDER BY r.price_per_night ASC;

-- Query 4: Lấy danh sách phòng cho admin (bao gồm cả hidden rooms)
SELECT
    r.room_id,
    r.room_number,
    r.room_name,
    r.status,
    r.is_visible,
    r.price_per_night,
    bl.layout_name
FROM rooms r
LEFT JOIN bed_layouts bl ON r.bed_layout_id = bl.bed_layout_id
WHERE r.status IN ('available', 'occupied', 'maintenance')
ORDER BY r.room_id ASC;
```

**Explanation:**

- **Query 1 (Read Room)**: Fetches current room visibility state before toggle
- **Query 2 (Update Visibility)**: Executed within @Transactional context, updates is_visible field (BIT column: 0=false, 1=true)
- **Query 3 (Public Search)**: Filters by is_visible=1 to show only visible rooms to customers
- **Query 4 (Admin List)**: Displays all rooms regardless of visibility for admin management, includes is_visible column for toggle button state

**Result Example:**

| Query | Result                                                        |
| ----- | ------------------------------------------------------------- |
| 1     | room_id=101, is_visible=1 (currently shown)                   |
| 2     | (1 row affected)                                              |
| 3     | List of visible rooms only (is_visible=1, status='available') |
| 4     | List of all rooms with is_visible column                      |

---

## Business Rules Summary

**Visibility Rules:**

| Scenario                       | is_visible | status      | Visible in Public Search? | Visible in Admin Table? |
| ------------------------------ | ---------- | ----------- | ------------------------- | ----------------------- |
| Normal room (shown)            | 1 (true)   | available   | ✅ Yes                    | ✅ Yes                  |
| Hidden room (admin action)     | 0 (false)  | available   | ❌ No                     | ✅ Yes (with badge)     |
| Occupied room (shown)          | 1 (true)   | occupied    | ❌ No (status filter)     | ✅ Yes                  |
| Maintenance room (auto-hidden) | 1 (true)   | maintenance | ❌ No (status filter)     | ✅ Yes                  |
| Maintenance + manually hidden  | 0 (false)  | maintenance | ❌ No                     | ✅ Yes                  |

**Key Points:**

- **is_visible flag**: Controls admin-driven visibility toggle (manual hide/show)
- **status filter**: Automatically excludes occupied and maintenance rooms from public search
- **Admin view**: Always shows all rooms regardless of is_visible or status
- **Public view**: Only shows rooms where is_visible=1 AND status='available'

---

**End of Show/Hide Room Design Document**
