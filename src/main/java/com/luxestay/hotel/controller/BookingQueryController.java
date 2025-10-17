package com.luxestay.hotel.controller;

import com.luxestay.hotel.dto.booking.BookingSummary;
import com.luxestay.hotel.dto.PagedResponse;
import com.luxestay.hotel.service.AuthService;
import com.luxestay.hotel.service.BookingQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingQueryController {

    private final AuthService authService;
    private final BookingQueryService bookingQueryService;

    // GET /api/bookings/my?status=&page=&size=
    // Lấy lịch sử đặt phòng của KH đang đăng nhập (có lọc status)
    @GetMapping("/my")
    public ResponseEntity<PagedResponse<BookingSummary>> myBookings(
            @RequestHeader("X-Auth-Token") String token,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ){
        Integer accountId = authService.verify(token)
                .orElseThrow(() -> new IllegalArgumentException("Bạn cần đăng nhập"));
        Pageable pageable = PageRequest.of(Math.max(page,0), Math.max(size,1), Sort.by("createdAt").descending());
        return ResponseEntity.ok(bookingQueryService.listMine(accountId, status, pageable));
    }

    // Lấy danh sách yêu cầu hủy đặt phòng của Admin/Staff
    @GetMapping("/cancel-requests")
    public ResponseEntity<PagedResponse<BookingSummary>> listCancelRequests(
            @RequestHeader("X-Auth-Token") String token,
            @RequestParam(value="page", defaultValue="0") int page,
            @RequestParam(value="size", defaultValue="10") int size
    ){
        var acc = authService.requireAccount(token);
        String role = acc.getRole()!=null ? acc.getRole().getName() : "";
        if (!"admin".equalsIgnoreCase(role) && !"staff".equalsIgnoreCase(role)) {
            return ResponseEntity.status(403)
                    .body(new PagedResponse<>(java.util.List.of(), 0, page, size));
        }
        var pageable = PageRequest.of(Math.max(page,0), Math.max(size,1), Sort.by("createdAt").descending());
        return ResponseEntity.ok(bookingQueryService.listMine(null, "cancel_requested", pageable));
    }

}
