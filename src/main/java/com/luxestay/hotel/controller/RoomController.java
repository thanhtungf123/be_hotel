package com.luxestay.hotel.controller;

import com.luxestay.hotel.dto.PagedResponse;
import com.luxestay.hotel.dto.RoomDetail;
import com.luxestay.hotel.dto.RoomImageRequest;
import com.luxestay.hotel.dto.RoomSearchCriteria;
import com.luxestay.hotel.model.Room;
import com.luxestay.hotel.service.RoomService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
// CORS - cái này cấp quyền cho đường dẫn FE nếu không call được API từ FE tới BE
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://127.0.0.1:5173",
        "http://localhost:4173",
        "http://localhost:3000"
})
public class RoomController {
    private final RoomService roomService;
    public RoomController(RoomService roomService) { this.roomService = roomService; }

    @GetMapping
    public List<Room> getRooms() { return roomService.listRooms(); }

    @GetMapping("/search")
    public PagedResponse<Room> search(
            @RequestParam(name = "priceMax", required = false) Integer priceMax,
            @RequestParam(name = "guests",   required = false) Integer guests,
            @RequestParam(name = "types",    required = false) String types,
            @RequestParam(name = "amenities",required = false) String amenities,
            @RequestParam(name = "sort",     required = false, defaultValue = "priceAsc") String sort,
            @RequestParam(name = "page",     required = false, defaultValue = "0") Integer page,
            @RequestParam(name = "size",     required = false, defaultValue = "10") Integer size
    ) {
        RoomSearchCriteria c = new RoomSearchCriteria();
        c.setPriceMax(priceMax);
        c.setGuests(guests);
        c.setSort(sort);
        c.setPage(page);
        c.setSize(size);

        if (types != null && !types.isBlank())
            c.setTypes(Arrays.stream(types.split(",")).map(String::trim).toList());
        if (amenities != null && !amenities.isBlank())
            c.setAmenities(Arrays.stream(amenities.split(",")).map(String::trim).toList());

        return roomService.search(c);
    }

    @GetMapping("/{id}")
    public RoomDetail getRoomDetail(@PathVariable("id") Long id) {
        try {
            return roomService.getDetail(id);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    /* --- Quản lý ảnh --- */
    @PostMapping("/{id}/images")
    public ResponseEntity<List<String>> addImages(
            @PathVariable("id") Long id,
            @RequestBody List<RoomImageRequest> body
    ){
        return ResponseEntity.ok(roomService.addImages(id, body));
    }

    @PutMapping("/{id}/images/{imageId}/primary")
    public ResponseEntity<Void> setPrimary(
            @PathVariable Long id, @PathVariable Integer imageId
    ){
        roomService.setPrimaryImage(id, imageId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @PathVariable Long id, @PathVariable Integer imageId
    ){
        roomService.deleteImage(id, imageId);
        return ResponseEntity.noContent().build();
    }
}
