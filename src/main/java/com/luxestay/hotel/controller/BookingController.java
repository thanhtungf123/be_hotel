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
@CrossOrigin(
    origins = {
        "http://localhost:5173", "http://127.0.0.1:5173",
        "http://localhost:4173", "http://localhost:3000"
    },
    allowedHeaders = {"X-Auth-Token","Authorization","Content-Type"},
    exposedHeaders = {"X-Auth-Token","Location"},
    maxAge = 3600
)
public class BookingController {
    private final BookingService bookingService;
    private final AuthService authService;

    // --- helper: lấy token từ X-Auth-Token hoặc Authorization: Bearer ---
    private String resolveToken(String xAuth, String authz) {
        if (xAuth != null && !xAuth.isBlank()) return xAuth;
        if (authz != null) {
            String a = authz.trim();
            if (a.regionMatches(true, 0, "Bearer ", 0, 7)) {
                return a.substring(7).trim();
            }
        }
        return null;
    }

    @PostMapping
    public ResponseEntity<BookingResponse> create(
            @RequestHeader(value = "X-Auth-Token", required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody BookingRequest req
    ) {
        // gom 2 header về 1 token
        if ((token == null || token.isBlank()) &&
            authorization != null &&
            authorization.toLowerCase().startsWith("bearer ")) {
            token = authorization.substring(7);
        }

        // Dùng requireAccount để thống nhất logic (như các controller khác)
        Account acc = authService.requireAccount(token);

        BookingResponse res = bookingService.create(acc.getId(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }
    // KH gửi yêu cầu hủy
    @PatchMapping("/{id}/request-cancel")
    public ResponseEntity<?> requestCancel(
            @PathVariable("id") Integer id,
            @RequestHeader(value = "X-Auth-Token", required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) CancelRequest body
    ){
        Account acc = authService.requireAccount(resolveToken(token, authorization));
        bookingService.requestCancel(id, acc.getId(), body!=null? body.getReason(): null);
        return ResponseEntity.ok(Map.of(
                "bookingId", id,
                "status", "cancel_requested",
                "message", "Đã gửi yêu cầu hủy. Vui lòng chờ phê duyệt."
        ));
    }

    // Admin/Staff duyệt/từ chối yêu cầu hủy
    @PatchMapping("/{id}/cancel-decision")
    public ResponseEntity<?> cancelDecision(
            @PathVariable("id") Integer id,
            @RequestHeader(value = "X-Auth-Token", required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody CancelDecisionRequest body
    ){
        Account staff = authService.requireAccount(resolveToken(token, authorization));
        String role = staff.getRole()!=null ? staff.getRole().getName() : "";
        if (!"admin".equalsIgnoreCase(role) && !"staff".equalsIgnoreCase(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message","Chỉ admin/staff được duyệt hủy"));
        }
        bookingService.decideCancel(id, staff.getId(), Boolean.TRUE.equals(body.getApprove()), body.getNote());
        return ResponseEntity.ok(Map.of("bookingId", id));
    }

    // Lịch sử của KH
    @GetMapping
    public ResponseEntity<PagedResponse<BookingSummary>> history(
            @RequestHeader(value = "X-Auth-Token", required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ){
        Account acc = authService.requireAccount(resolveToken(token, authorization));
        var res = bookingService.history(acc.getId(), status, page, size);
        return ResponseEntity.ok(res);
    }

    @PatchMapping("/{id}/approve-cancel")
    public ResponseEntity<?> approveCancelAlias(
            @PathVariable("id") Integer id,
            @RequestHeader(value = "X-Auth-Token", required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody CancelDecisionRequest body
    ) {
        Account staff = authService.requireAccount(resolveToken(token, authorization));
        String role = staff.getRole()!=null ? staff.getRole().getName() : "";
        if (!"admin".equalsIgnoreCase(role) && !"staff".equalsIgnoreCase(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message","Chỉ admin/staff được duyệt hủy"));
        }
        bookingService.decideCancel(id, staff.getId(), Boolean.TRUE.equals(body.getApprove()), body.getNote());
        return ResponseEntity.ok(Map.of("bookingId", id));
    }

    // Customer submit refund info
    @PostMapping("/{id}/refund-info")
    public ResponseEntity<?> submitRefundInfo(
            @PathVariable("id") Integer id,
            @RequestHeader(value = "X-Auth-Token", required = false) String token,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody RefundInfoRequest request
    ) {
        Account acc = authService.requireAccount(resolveToken(token, authorization));
        bookingService.submitRefundInfo(id, acc.getId(), request);
        return ResponseEntity.ok(Map.of(
                "bookingId", id,
                "message", "Đã gửi thông tin hoàn tiền thành công"
        ));
    }
}
