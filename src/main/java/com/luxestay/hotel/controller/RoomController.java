package com.luxestay.hotel.controller;

import com.luxestay.hotel.dto.PagedResponse;
import com.luxestay.hotel.dto.RoomAvailabilityRequest;
import com.luxestay.hotel.dto.RoomDetail;
import com.luxestay.hotel.dto.RoomImageRequest;
import com.luxestay.hotel.dto.RoomRecommendRequest;
import com.luxestay.hotel.dto.RoomRequest;
import com.luxestay.hotel.dto.RoomSearchCriteria;
import com.luxestay.hotel.dto.RoomStatusUpdateRequest;
import com.luxestay.hotel.model.Room;
import com.luxestay.hotel.service.RoomService;
import com.luxestay.hotel.util.AuthorizationHelper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.luxestay.hotel.repository.BookingRepository;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
// CORS - cái này cấp quyền cho đường dẫn FE nếu không call được API từ FE tới
// BE
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://127.0.0.1:5173",
        "http://localhost:4173",
        "http://localhost:3000"
})
@RequiredArgsConstructor
public class RoomController {
    private final RoomService roomService;
    private final AuthorizationHelper authHelper;
    private final BookingRepository bookingRepository; 

    @GetMapping
    public List<Room> getRooms() {
        return roomService.listRooms();
    }

    @GetMapping("/admin/all")
    public List<Room> getAllRoomsForAdmin(HttpServletRequest httpRequest) {
        // Allow both admin and staff to view rooms
        authHelper.requireAdminOrStaff(httpRequest);
        return roomService.listAllRoomsForAdmin();
    }



    @GetMapping("/search")
    public PagedResponse<Room> search(
            @RequestParam(name = "priceMax", required = false) Integer priceMax,
            @RequestParam(name = "priceMin", required = false) Integer priceMin,
            @RequestParam(name = "guests", required = false) Integer guests,
            @RequestParam(name = "types", required = false) String types,
            @RequestParam(name = "amenities", required = false) String amenities,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "sort", required = false, defaultValue = "priceAsc") String sort,
            @RequestParam(name = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(name = "size", required = false, defaultValue = "10") Integer size) {
        RoomSearchCriteria c = new RoomSearchCriteria();
        c.setPriceMax(priceMax);
        c.setPriceMin(priceMin);
        c.setGuests(guests);
        c.setSort(sort);
        c.setPage(page);
        c.setSize(size);

        if (types != null && !types.isBlank())
            c.setTypes(Arrays.stream(types.split(",")).map(String::trim).toList());
        if (amenities != null && !amenities.isBlank())
            c.setAmenities(Arrays.stream(amenities.split(",")).map(String::trim).toList());
        if (status != null && !status.isBlank())
            c.setStatus(Arrays.stream(status.split(",")).map(String::trim).toList());

        return roomService.search(c);
    }

    /**
     * API kiểm tra phòng trống theo ngày check-in, check-out, số khách
     * GET
     * /api/rooms/availability?checkIn=2025-10-20&checkOut=2025-10-22&guests=2&priceMin=1000000&priceMax=5000000
     */
    @GetMapping("/availability")
    public PagedResponse<Room> checkAvailability(
            @RequestParam(name = "checkIn", required = true) String checkIn,
            @RequestParam(name = "checkOut", required = true) String checkOut,
            @RequestParam(name = "guests", required = false) Integer guests,
            @RequestParam(name = "priceMin", required = false) Integer priceMin,
            @RequestParam(name = "priceMax", required = false) Integer priceMax) {

        try {
            RoomAvailabilityRequest req = new RoomAvailabilityRequest();
            req.setCheckIn(LocalDate.parse(checkIn));
            req.setCheckOut(LocalDate.parse(checkOut));
            req.setGuests(guests);
            req.setPriceMin(priceMin);
            req.setPriceMax(priceMax);

            return roomService.checkAvailability(req);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    @GetMapping("/recommend")
    public List<Room> getRecommendedRooms(
            @RequestParam(name = "accountId", required = false) Long accountId,
            @RequestParam(name = "type", required = false, defaultValue = "auto") String type,
            @RequestParam(name = "limit", required = false, defaultValue = "5") Integer limit) {

        RoomRecommendRequest req = new RoomRecommendRequest(accountId, type, limit);
        return roomService.recommendRooms(req);
    }

    @GetMapping("/{id}")
    public RoomDetail getRoomDetail(@PathVariable("id") Long id) {
        try {
            return roomService.getDetail(id);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    /*
     * ===========================
     * ADMIN CRUD OPERATIONS
     * ===========================
     */

    /**
     * Create new room (Admin only)
     * POST /api/rooms
     * Requires: X-Auth-Token header with admin role
     */
    @PostMapping
    public ResponseEntity<Room> createRoom(
            @RequestBody RoomRequest req,
            HttpServletRequest httpRequest) {
        // Require admin authorization
        authHelper.requireAdmin(httpRequest);

        try {
            Room room = roomService.createRoom(req);
            return ResponseEntity.status(HttpStatus.CREATED).body(room);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    /**
     * Update existing room (Admin only)
     * PUT /api/rooms/{id}
     * Requires: X-Auth-Token header with admin role
     */
    @PutMapping("/{id}")
    public ResponseEntity<Room> updateRoom(
            @PathVariable("id") Long id,
            @RequestBody RoomRequest req,
            HttpServletRequest httpRequest) {
        // Require admin authorization
        authHelper.requireAdmin(httpRequest);

        try {
            Room room = roomService.updateRoom(id, req);
            return ResponseEntity.ok(room);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }
    }

    /**
     * Delete room - soft delete (Admin only)
     * DELETE /api/rooms/{id}
     * Requires: X-Auth-Token header with admin role
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoom(
            @PathVariable("id") Long id,
            HttpServletRequest httpRequest) {
        // Require admin authorization
        authHelper.requireAdmin(httpRequest);

        try {
            roomService.deleteRoom(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    /**
     * Toggle room visibility (Admin only)
     * PATCH /api/rooms/{id}/visibility
     * Requires: X-Auth-Token header with admin role
     * Body: { "isVisible": true/false }
     */
    @PatchMapping("/{id}/visibility")
    public ResponseEntity<Void> toggleVisibility(
            @PathVariable("id") Long id,
            @RequestBody java.util.Map<String, Boolean> body,
            HttpServletRequest httpRequest) {
        // Require admin authorization
        authHelper.requireAdmin(httpRequest);

        try {
            Boolean isVisible = body.get("isVisible");
            if (isVisible == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "isVisible field is required");
            }
            roomService.toggleVisibility(id, isVisible);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateRoomStatus(
            @PathVariable Long id,
            @RequestBody RoomStatusUpdateRequest request,
            HttpServletRequest httpRequest) {
        try {
            authHelper.requireAdmin(httpRequest);
            roomService.updateRoomStatus(id, request.getStatus(), request.getReason());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            // Return error message in response body
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    /* --- Quản lý ảnh --- */
    @PostMapping("/{id}/images")
    public ResponseEntity<List<String>> addImages(
            @PathVariable("id") Long id,
            @RequestBody List<RoomImageRequest> body) {
        return ResponseEntity.ok(roomService.addImages(id, body));
    }

    @PutMapping("/{id}/images/{imageId}/primary")
    public ResponseEntity<Void> setPrimary(
            @PathVariable Long id, @PathVariable Integer imageId) {
        roomService.setPrimaryImage(id, imageId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @PathVariable Long id, @PathVariable Integer imageId) {
        roomService.deleteImage(id, imageId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/availability")
    public Map<String, Object> roomAvailabilityPreview(
            @PathVariable("id") Long id,
            @RequestParam("start") String startStr,
            @RequestParam("end")   String endStr
    ) {
        var start = LocalDate.parse(startStr);
        var end   = LocalDate.parse(endStr);

        var overlaps = bookingRepository.findOverlaps(id.intValue(), start, end);

        if (overlaps.isEmpty()) {
            return Map.of("available", true, "availableFrom", start, "blocked", List.of());
        }

        // build danh sách khoảng bị chặn
        var blocked = overlaps.stream().map(b -> Map.of(
                "start", b.getCheckIn().toString(),
                "end",   b.getCheckOut().toString()
        )).toList();

        var availableFrom = overlaps.stream()
                .map(b -> b.getCheckOut())
                .max(LocalDate::compareTo)
                .orElse(start);

        return Map.of(
                "available", false,
                "availableFrom", availableFrom,
                "blocked", blocked
        );
    }

    /**
     * API lấy số lượng phòng theo từng amenities
     * GET /api/rooms/amenities/counts
     */
    @GetMapping("/amenities/counts")
    public Map<String, Long> getAmenityCounts() {
        return roomService.getAmenityCounts();
    }
}
