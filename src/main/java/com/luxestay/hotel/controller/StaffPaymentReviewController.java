// src/main/java/com/luxestay/hotel/controller/StaffPaymentReviewController.java
package com.luxestay.hotel.controller;

import com.luxestay.hotel.dto.PagedResponse;
import com.luxestay.hotel.dto.payment.PaymentReviewItem;
import com.luxestay.hotel.model.Account;
import com.luxestay.hotel.model.entity.BookingEntity;
import com.luxestay.hotel.model.entity.RoomEntity;
import com.luxestay.hotel.repository.BookingRepository;
import com.luxestay.hotel.repository.PaymentRepository;
import com.luxestay.hotel.repository.RoomRepository;
import com.luxestay.hotel.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/staff/payment-review")
@RequiredArgsConstructor
public class StaffPaymentReviewController {

    private final AuthService authService;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final RoomRepository roomRepository;

    /** Đọc token từ header, bắt buộc role staff|admin */
    private Account ensureStaffOrAdmin(String token) {
        Account acc = authService.requireAccount(token); // <-- KHÁC BIỆT Ở ĐÂY
        String role = acc.getRole() != null ? acc.getRole().getName() : "";
        if (!"admin".equalsIgnoreCase(role) && !"staff".equalsIgnoreCase(role)) {
            throw new IllegalArgumentException("Chỉ staff/admin");
        }
        return acc;
    }

    @GetMapping
    public PagedResponse<PaymentReviewItem> list(
            @RequestHeader("X-Auth-Token") String token,        // <-- NHẬN TOKEN
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        ensureStaffOrAdmin(token);
        var pg = bookingRepository.findPendingPaymentReviews(PageRequest.of(page, size));

        List<PaymentReviewItem> items = pg.getContent().stream().map(b -> {
            var it = new PaymentReviewItem();
            it.bookingId     = b.getId();
            it.roomName      = b.getRoom() != null ? b.getRoom().getRoomName() : null;
            it.checkIn       = b.getCheckIn();
            it.checkOut      = b.getCheckOut();
            it.totalPrice    = b.getTotalPrice();
            it.depositAmount = b.getDepositAmount();
            it.amountPaid    = paymentRepository.sumPaidByBooking(b.getId());
            it.paymentState  = b.getPaymentState();
            return it;
        }).toList();

        return new PagedResponse<>(items, pg.getTotalElements(), pg.getNumber(), pg.getSize());
    }

    @PostMapping("/{bookingId}/approve")
    public ResponseEntity<?> approve(
            @RequestHeader("X-Auth-Token") String token,        // <-- NHẬN TOKEN
            @PathVariable Integer bookingId,
            @RequestParam(required = false) String note
    ) {
        var acc = ensureStaffOrAdmin(token);

        BookingEntity b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đặt phòng"));

        if (!"deposit_paid".equalsIgnoreCase(b.getPaymentState())
                && !"paid_in_full".equalsIgnoreCase(b.getPaymentState())) {
            throw new IllegalStateException("Đơn chưa có giao dịch hợp lệ để duyệt");
        }

        // Đánh dấu đã review và xác nhận
        b.setPaymentReviewedAt(LocalDateTime.now());
        b.setPaymentReviewedBy(acc.getId());
        if (note != null && !note.isBlank()) b.setPaymentNote(note);
        b.setStatus("confirmed");
        bookingRepository.save(b);

        // Block phòng
        RoomEntity r = b.getRoom();
        if (r != null) {
            r.setStatus("reserved");
            roomRepository.save(r);
        }

        return ResponseEntity.ok().build();
    }
}
