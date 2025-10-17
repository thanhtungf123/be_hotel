package com.luxestay.hotel.controller;

import com.luxestay.hotel.dto.booking.BookingRequest;
import com.luxestay.hotel.dto.booking.BookingResponse;
import com.luxestay.hotel.service.AuthService;
import com.luxestay.hotel.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@CrossOrigin(origins = {
        "http://localhost:5173", "http://127.0.0.1:5173",
        "http://localhost:4173", "http://localhost:3000"
})
public class BookingController {

    private final BookingService bookingService;
    private final AuthService authService;

    @PostMapping
    public ResponseEntity<BookingResponse> create(
            @RequestHeader(value = "X-Auth-Token", required = false) String token,
            @RequestBody BookingRequest req
    ) {
        var accountIdOpt = authService.verify(token);
        if (accountIdOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        BookingResponse res = bookingService.create(accountIdOpt.get(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }
}
