package com.luxestay.hotel.controller;

import com.luxestay.hotel.dto.PagedResponse;
import com.luxestay.hotel.dto.booking.*;
import com.luxestay.hotel.model.Account;
import com.luxestay.hotel.service.AuthService;
import com.luxestay.hotel.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
        if (accountIdOpt.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        BookingResponse res = bookingService.create(accountIdOpt.get(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    /** KH gửi yêu cầu hủy */
    @PatchMapping("/{id}/request-cancel")
    public ResponseEntity<?> requestCancel(
            @PathVariable Integer id,
            @RequestHeader("X-Auth-Token") String token,
            @RequestBody(required = false) CancelRequest body
    ){
        Account acc = authService.requireAccount(token);
        bookingService.requestCancel(id, acc.getId(), body!=null? body.getReason(): null);
        return ResponseEntity.ok(Map.of(
                "bookingId", id,
                "status", "cancellation_requested",
                "message", "Đã gửi yêu cầu hủy. Vui lòng chờ phê duyệt."
        ));
    }

    /** Admin/Staff duyệt/từ chối yêu cầu hủy */
    @PatchMapping("/{id}/cancel-decision")
    public ResponseEntity<?> cancelDecision(
            @PathVariable Integer id,
            @RequestHeader("X-Auth-Token") String token,
            @RequestBody CancelDecisionRequest body
    ){
        Account staff = authService.requireAccount(token);
        String role = staff.getRole()!=null ? staff.getRole().getName() : "";
        if (!"admin".equalsIgnoreCase(role) && !"staff".equalsIgnoreCase(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message","Chỉ admin/staff được duyệt hủy"));
        }
        bookingService.decideCancel(id, staff.getId(), Boolean.TRUE.equals(body.getApprove()), body.getNote());
        return ResponseEntity.ok(Map.of("bookingId", id));
    }

    /** Lịch sử của KH đang đăng nhập (có lọc status) */
    @GetMapping
    public ResponseEntity<PagedResponse<BookingSummary>> history(
            @RequestHeader("X-Auth-Token") String token,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ){
        Account acc = authService.requireAccount(token);
        var res = bookingService.history(acc.getId(), status, page, size);
        return ResponseEntity.ok(res);
    }
}
