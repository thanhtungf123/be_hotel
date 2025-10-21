# II. Code Designs - Room Status Update Feature

## 1. Update Status Room (Cập nhật trạng thái phòng)

_[Provide the detailed design for the function <Feature/Function Name>. It include Class Diagram, Class Specifications, and Sequence Diagram(s)]_

---

### a. Class Diagram

_[This part presents the class diagram for the relevant feature]_

**![Class Diagram - Room Status Update](room_status_update_class_diagram.png)**

_Note: Export PlantUML diagram from `be_hotel/docs/diagrams/room_status_update_class_diagram.puml` và insert image vào đây_

---

### b. Class Specifications

_[Provide the description for each class and the methods in each class, following the table format as below]_

#### RoomStatusUpdateRequest

| No  | Method      | Description                                                                                                                        |
| --- | ----------- | ---------------------------------------------------------------------------------------------------------------------------------- |
| 01  | status      | New room status to update: "available" (ready for booking), "occupied" (currently in use), "maintenance" (closed for repairs).     |
| 02  | reason      | Optional explanation for status change, especially required for "maintenance" status (e.g., "Sửa chữa điều hòa", "Thay thảm mới"). |
| 03  | getters     | Standard getter methods to retrieve field values (getStatus(), getReason()).                                                       |
| 04  | setters     | Standard setter methods to update field values (setStatus(String status), setReason(String reason)).                               |
| 05  | constructor | Default constructor initializes empty fields. Parameterized constructor accepts status and reason.                                 |

#### Room

| No  | Method     | Description                                                                                                  |
| --- | ---------- | ------------------------------------------------------------------------------------------------------------ |
| 01  | id         | Unique room identifier (primary key).                                                                        |
| 02  | roomNumber | Room number displayed to users (e.g., "101", "102", "201").                                                  |
| 03  | name       | Room name or title (e.g., "Standard Twin", "Deluxe Ocean View").                                             |
| 04  | status     | Current room status: "available" (ready for booking), "occupied" (currently in use), "maintenance" (closed). |
| 05  | priceVnd   | Price per night in Vietnamese Dong (VND).                                                                    |
| 06  | capacity   | Maximum number of guests the room can accommodate.                                                           |
| 07  | isVisible  | Visibility flag: true = shown in search results, false = hidden from public view.                            |
| 08  | getters    | Standard getter methods for all fields.                                                                      |
| 09  | setters    | Standard setter methods for all fields.                                                                      |

#### RoomController

| No  | Method                        | Description                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| --- | ----------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | roomService                   | Dependency injection of RoomService interface for business logic processing.                                                                                                                                                                                                                                                                                                                                                                              |
| 02  | authHelper                    | Dependency injection of AuthorizationHelper for admin authorization checking.                                                                                                                                                                                                                                                                                                                                                                             |
| 03  | updateRoomStatus(id, request) | **Input:** id (Long, room identifier), request (RoomStatusUpdateRequest with status and reason)<br>**Output:** ResponseEntity&lt;Room&gt; - JSON response with updated room or error message<br>**Processing:** Validates admin authorization, extracts status and reason from request body, calls roomService.updateRoomStatus(), returns updated room as JSON. Catches IllegalArgumentException for validation errors and returns HTTP 400 Bad Request. |

#### RoomService (Interface)

| No  | Method                                  | Description                                                                                                                                                                                                                                   |
| --- | --------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | updateRoomStatus(id, newStatus, reason) | **Input:** id (Long, room identifier), newStatus (String), reason (String, optional)<br>**Output:** void<br>**Processing:** Abstract method defining room status update contract to be implemented by service layer with transaction support. |

#### RoomServiceImpl

| No  | Method                                       | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| --- | -------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | roomRepository                               | Dependency injection of RoomRepository for room data access.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| 02  | bookingRepository                            | Dependency injection of BookingRepository for checking active bookings.                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| 03  | updateRoomStatus(id, newStatus, reason)      | **Input:** id (Long), newStatus (String), reason (String)<br>**Output:** void<br>**Processing:** Main status update orchestrator with @Transactional annotation. Validates newStatus against allowed values ["available", "occupied", "maintenance"], fetches RoomEntity from repository, checks if status already matches (no-op), extracts currentStatus, calls validateStatusTransition() for business rule validation, updates room.status in entity, saves to database via repository.                                                     |
| 04  | validateStatusTransition(room, current, new) | **Input:** room (RoomEntity), currentStatus (String), newStatus (String)<br>**Output:** void (throws IllegalArgumentException on validation failure)<br>**Processing:** State machine enforcer. Implements business rules: (1) available→occupied: must have active booking, (2) occupied→available: cannot have active booking, (3) occupied→maintenance: cannot have active booking, (4) maintenance→occupied: must have active booking. Calls hasActiveBooking() for each rule. Throws descriptive error messages for Vietnamese UI display. |
| 05  | hasActiveBooking(roomId)                     | **Input:** roomId (Integer)<br>**Output:** boolean<br>**Processing:** Queries BookingRepository.existsByRoom_IdAndStatusInAndCheckOutAfter() with roomId, status list ['pending', 'confirmed', 'checked_in'], and checkOutDate > today. Returns true if any active booking exists, false otherwise. Used to prevent status changes that violate booking constraints.                                                                                                                                                                            |

#### RoomRepository (Interface)

| No  | Method       | Description                                                                                                                                                                                                                                                     |
| --- | ------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 01  | findById(id) | **Input:** id (Long, room identifier)<br>**Output:** Optional&lt;RoomEntity&gt;<br>**Processing:** JPA built-in method. Fetches single room entity by primary key. Returns Optional.empty() if not found.                                                       |
| 02  | save(room)   | **Input:** room (RoomEntity with updated status field)<br>**Output:** RoomEntity (saved entity with updated timestamp)<br>**Processing:** JPA built-in method. Persists room entity changes to database. Used within @Transactional context for status updates. |

#### BookingRepository (Interface)

| No  | Method                                                             | Description                                                                                                                                                                                                                                                                                                                                                              |
| --- | ------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 01  | existsByRoom_IdAndStatusInAndCheckOutAfter(roomId, statuses, date) | **Input:** roomId (Integer), statuses (List&lt;String&gt; = ['pending', 'confirmed', 'checked_in']), date (LocalDate = today)<br>**Output:** boolean<br>**Processing:** JPA query method. Checks if any booking exists for the given room with active statuses and checkOut date in the future. Returns true if conflict exists, false if room is free to update status. |

---

### c. Sequence Diagram

_[Provide the sequence diagram(s) for the feature, see the sample below]_

**![Sequence Diagram - Room Status Update Complete Flow](room_status_update_sequence_combined.png)**

_Note: Export PlantUML diagram from `be_hotel/docs/diagrams/room_status_update_sequence_combined.puml` và insert image vào đây_

**Flow Description:**

This unified sequence diagram covers all status update scenarios including success cases, validation failures, and maintenance operations.

#### Main Flow:

1. **Initiation:**

   - Admin selects a room from the management table
   - Admin changes status via dropdown (available/occupied/maintenance)
   - If status = "maintenance", frontend prompts for reason (optional)
   - Frontend confirms action via window.confirm()

2. **Authorization:**

   - Frontend retrieves X-Auth-Token from localStorage
   - Sends PATCH request to `/api/rooms/{id}/status` with `{"status": "...", "reason": "..."}`
   - RoomController validates admin authorization via authHelper.requireAdmin()
   - If not admin → returns HTTP 403 Forbidden (flow ends)

3. **Validation:**

   - Service validates new status is in allowed list ["available", "occupied", "maintenance"]
   - If invalid → returns HTTP 400 Bad Request
   - Service fetches room from RoomRepository.findById(id)
   - If not found → returns HTTP 404 Not Found
   - If currentStatus == newStatus → no-op, returns HTTP 200 (no changes)

4. **State Transition Validation (Critical Logic):**

   **Case A: available → occupied OR maintenance → occupied**

   - Rule: Must have ACTIVE booking
   - Calls BookingRepository to check for active bookings (status: pending/confirmed/checked_in, check_out > today)
   - If NO booking → throws IllegalArgumentException: "Phòng chưa có booking"
   - If has booking → validation passed ✅

   **Case B: occupied → available OR occupied → maintenance**

   - Rule: Must NOT have ACTIVE booking
   - Calls BookingRepository to check for active bookings
   - If has booking → throws IllegalArgumentException: "Phòng đang có booking active"
   - If NO booking → validation passed ✅

   **Case C: available → maintenance OR maintenance → available**

   - Rule: Always allowed (no constraints)
   - Validation passed immediately ✅

5. **Update Execution:**

   - If validation fails → Controller catches exception, returns HTTP 400 with error message
   - Frontend displays error alert, reverts UI changes (dropdown returns to old value)
   - If validation passed → Service updates room.status in entity
   - Calls RoomRepository.save(room) with @Transactional context
   - Database executes: `UPDATE rooms SET status=? WHERE room_id=?`

6. **Success Response:**
   - Controller maps updated RoomEntity to Room DTO
   - Returns HTTP 200 OK with updated room JSON
   - Frontend updates table row:
     - Status badge color: "Có sẵn" (green), "Có người ở" (blue), "Bảo trì" (orange)
     - Dropdown reflects new value
   - Admin sees confirmation alert: "Cập nhật trạng thái thành công!"
   - If status = "maintenance" → room automatically hidden from public search (is_visible filter)

#### Error Handling Summary:

| Error Type               | HTTP Status     | User Message                                                      |
| ------------------------ | --------------- | ----------------------------------------------------------------- |
| Not Admin                | 403 Forbidden   | "Không có quyền truy cập"                                         |
| Invalid Status           | 400 Bad Request | "Trạng thái không hợp lệ"                                         |
| Room Not Found           | 404 Not Found   | "Không tìm thấy phòng"                                            |
| No Booking (need one)    | 400 Bad Request | "Không thể chuyển sang 'occupied': Phòng chưa có booking"         |
| Has Booking (can't have) | 400 Bad Request | "Không thể chuyển sang 'available': Phòng đang có booking active" |

---

### d. Database Queries

_[Provide the detailed SQL (select, insert, update...) which are used in implementing the function/screen]_

**Purpose:** Database queries used in room status update workflow, including validation, read, and update operations.

**SQL Queries:**

```sql
-- Query 1: Lấy thông tin phòng hiện tại để kiểm tra trước khi cập nhật
SELECT
    r.room_id,
    r.room_number,
    r.room_name,
    r.status,
    r.price_per_night,
    r.capacity,
    r.is_visible,
    r.bed_layout_id
FROM rooms r
WHERE r.room_id = 101;  -- Thay 101 bằng room ID cụ thể

-- Query 2: Kiểm tra phòng có booking đang active (State Machine Validation)
SELECT COUNT(*) AS has_active_booking
FROM bookings b
WHERE b.room_id = 101  -- Thay 101 bằng room ID cụ thể
  AND b.status IN ('pending', 'confirmed', 'checked_in')
  AND b.check_out > CAST(GETDATE() AS DATE);

-- Query 3: Cập nhật trạng thái phòng sau khi validation thành công
UPDATE rooms
SET status = 'occupied'  -- Giá trị mới: 'available', 'occupied', 'maintenance'
WHERE room_id = 101;     -- Thay 101 bằng room ID cụ thể

-- Query 4: Lấy danh sách phòng cho admin management table
SELECT
    r.room_id,
    r.room_number,
    r.room_name,
    r.status,
    r.price_per_night,
    r.capacity,
    r.is_visible,
    bl.layout_name
FROM rooms r
LEFT JOIN bed_layouts bl ON r.bed_layout_id = bl.bed_layout_id
WHERE r.status IN ('available', 'occupied', 'maintenance')
ORDER BY r.room_id ASC;
```

**Explanation:**

- **Query 1 (Read Room)**: Fetches current room status to compare with new status (prevent no-op updates)
- **Query 2 (Validation)**: Checks for active bookings to enforce state machine rules
  - available → occupied: Requires count > 0 (must have booking)
  - occupied → available: Requires count = 0 (must NOT have booking)
  - occupied → maintenance: Requires count = 0 (must NOT have booking)
  - maintenance → occupied: Requires count > 0 (must have booking)
  - available ↔ maintenance: No check needed (always allowed)
- **Query 3 (Update)**: Executed within @Transactional context, rollback if validation fails
- **Query 4 (Admin List)**: Displays all rooms regardless of visibility for admin management

**Result Example:**

| Query | Result                                                     |
| ----- | ---------------------------------------------------------- |
| 1     | room_id=101, status="available", room_number="101"         |
| 2     | has_active_booking=0 (no booking) or 2 (has bookings)      |
| 3     | (1 row affected)                                           |
| 4     | List of rooms with id, number, name, status, price, layout |

---

## State Machine Rules Summary

**State Transition Matrix:**

| From        | To          | Validation Rule              | Error Message (Vietnamese)                                          |
| ----------- | ----------- | ---------------------------- | ------------------------------------------------------------------- |
| available   | occupied    | Must have active booking     | "Không thể chuyển sang 'occupied': Phòng chưa có booking"           |
| available   | maintenance | Always allowed               | -                                                                   |
| occupied    | available   | Must NOT have active booking | "Không thể chuyển sang 'available': Phòng đang có booking active"   |
| occupied    | maintenance | Must NOT have active booking | "Không thể chuyển sang 'maintenance': Phòng đang có booking active" |
| maintenance | available   | Always allowed               | -                                                                   |
| maintenance | occupied    | Must have active booking     | "Không thể chuyển sang 'occupied': Phòng chưa có booking"           |

---

**End of Room Status Update Design Document**
