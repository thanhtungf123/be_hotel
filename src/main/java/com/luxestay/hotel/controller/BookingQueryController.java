// controller/BookingQueryController.java
package com.luxestay.hotel.controller;

import com.luxestay.hotel.dto.PagedResponse;
import com.luxestay.hotel.dto.booking.BookingSummary;
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

    @GetMapping("/my")
    public ResponseEntity<PagedResponse<BookingSummary>> myBookings(
            @RequestHeader("X-Auth-Token") String token,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        Integer accountId = authService.verify(token)
                .orElseThrow(() -> new IllegalArgumentException("Bạn cần đăng nhập"));
        Pageable pageable = PageRequest.of(Math.max(page,0), Math.max(size,1), Sort.by("createdAt").descending());
        return ResponseEntity.ok(bookingQueryService.listMine(accountId, status, pageable));
    }
}
