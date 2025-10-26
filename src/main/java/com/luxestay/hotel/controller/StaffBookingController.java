package com.luxestay.hotel.controller;

import com.luxestay.hotel.model.Account;
import com.luxestay.hotel.model.entity.BookingEntity;
import com.luxestay.hotel.model.entity.RoomEntity;
import com.luxestay.hotel.repository.BookingRepository;
import com.luxestay.hotel.repository.RoomRepository;
import com.luxestay.hotel.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/staff/bookings")
@RequiredArgsConstructor
public class StaffBookingController {

    // Giờ check-in mặc định (dùng để tính no-show)
    private static final int CHECKIN_HOUR = 14;

    private final AuthService authService;
    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;

    /** Chỉ cho phép role staff|admin */
    private void ensureStaffOrAdmin(Account acc){
        String role = acc.getRole()!=null ? acc.getRole().getName() : "";
        if (!"admin".equalsIgnoreCase(role) && !"staff".equalsIgnoreCase(role)) {
            throw new IllegalArgumentException("Chỉ staff/admin");
        }
    }

    /** Staff xác nhận: đã thanh toán cọc → booking confirmed, room reserved */
    @PatchMapping("/{id}/verify-deposit")
    public ResponseEntity<?> verifyDeposit(@RequestHeader("X-Auth-Token") String token,
                                           @PathVariable Integer id){
        Account acc = authService.requireAccount(token);
        ensureStaffOrAdmin(acc);

        BookingEntity b = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đặt phòng"));

        b.setPaymentState("deposit_paid");
        b.setStatus("confirmed");
        bookingRepository.save(b);

        RoomEntity r = b.getRoom();
        if (r != null) {
            r.setStatus("reserved");
            roomRepository.save(r);
        }
        return ResponseEntity.ok(Map.of(
                "bookingId", id,
                "paymentState", "deposit_paid",
                "status", "confirmed"
        ));
    }

    /** Staff xác nhận: đã thanh toán toàn bộ → booking confirmed, room reserved */
    @PatchMapping("/{id}/verify-full")
    public ResponseEntity<?> verifyFull(@RequestHeader("X-Auth-Token") String token,
                                        @PathVariable Integer id){
        Account acc = authService.requireAccount(token);
        ensureStaffOrAdmin(acc);

        BookingEntity b = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đặt phòng"));

        b.setPaymentState("paid_in_full");
        b.setStatus("confirmed");
        bookingRepository.save(b);

        RoomEntity r = b.getRoom();
        if (r != null) {
            r.setStatus("reserved");
            roomRepository.save(r);
        }
        return ResponseEntity.ok(Map.of(
                "bookingId", id,
                "paymentState", "paid_in_full",
                "status", "confirmed"
        ));
    }

    /** Check-in: chuyển booking → checked_in, room → occupied */
    @PostMapping("/{id}/check-in")
    public ResponseEntity<?> checkIn(@RequestHeader("X-Auth-Token") String token,
                                     @PathVariable Integer id){
        Account acc = authService.requireAccount(token);
        ensureStaffOrAdmin(acc);

        BookingEntity b = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đặt phòng"));

        if (!"confirmed".equalsIgnoreCase(b.getStatus())) {
            throw new IllegalStateException("Chỉ check-in khi booking ở trạng thái confirmed");
        }

        b.setStatus("checked_in");
        bookingRepository.save(b);

        RoomEntity r = b.getRoom();
        if (r != null) {
            r.setStatus("occupied");
            roomRepository.save(r);
        }
        return ResponseEntity.ok(Map.of("bookingId", id, "status", "checked_in"));
    }

    /** Check-out: booking → checked_out, room → available */
    @PostMapping("/{id}/check-out")
    public ResponseEntity<?> checkOut(@RequestHeader("X-Auth-Token") String token,
                                      @PathVariable Integer id){
        Account acc = authService.requireAccount(token);
        ensureStaffOrAdmin(acc);

        BookingEntity b = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đặt phòng"));

        b.setStatus("checked_out");
        bookingRepository.save(b);

        RoomEntity r = b.getRoom();
        if (r != null) {
            r.setStatus("available");
            roomRepository.save(r);
        }
        return ResponseEntity.ok(Map.of("bookingId", id, "status", "checked_out"));
    }

    /** No-show (sau giờ check-in + 5h): booking → cancelled, room → available */
    @PostMapping("/{id}/mark-no-show")
    public ResponseEntity<?> markNoShow(@RequestHeader("X-Auth-Token") String token,
                                        @PathVariable Integer id){
        Account acc = authService.requireAccount(token);
        ensureStaffOrAdmin(acc);

        BookingEntity b = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đặt phòng"));

        if (!"confirmed".equalsIgnoreCase(b.getStatus())) {
            throw new IllegalStateException("Chỉ áp dụng cho đơn đã xác nhận");
        }
        if (b.getCheckIn() == null) {
            throw new IllegalStateException("Thiếu ngày check-in");
        }

        // +5 giờ kể từ giờ check-in mặc định (14:00)
        LocalDateTime threshold = b.getCheckIn().atTime(CHECKIN_HOUR, 0).plusHours(5);
        if (LocalDateTime.now().isBefore(threshold)) {
            throw new IllegalStateException("Chỉ được đánh no-show sau giờ check-in + 5h");
        }

        b.setStatus("cancelled");
        b.setCancelReason((b.getCancelReason()==null?"":"\n")
                + "Đánh dấu no-show bởi staff vào " + LocalDateTime.now());
        bookingRepository.save(b);

        RoomEntity r = b.getRoom();
        if (r != null) {
            r.setStatus("available");
            roomRepository.save(r);
        }
        return ResponseEntity.ok(Map.of("bookingId", id, "status", "cancelled"));
    }
}
