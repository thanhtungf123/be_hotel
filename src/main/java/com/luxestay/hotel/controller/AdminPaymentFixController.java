package com.luxestay.hotel.controller;

import com.luxestay.hotel.model.entity.BookingEntity;
import com.luxestay.hotel.repository.BookingRepository;
import com.luxestay.hotel.repository.PaymentRepository;
import com.luxestay.hotel.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * ADMIN UTILITY: Endpoint để fix payment state và status cho các booking bị lỗi.
 * ⚠️ CHỈ DÙNG ĐỂ DEBUG VÀ FIX DATA - XÓA TRƯỚC KHI PRODUCTION
 */
@RestController
@RequestMapping("/api/admin/payment-fix")
@RequiredArgsConstructor
public class AdminPaymentFixController {

    private final BookingService bookingService;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    /**
     * Manually trigger onPaymentCaptured for a specific booking
     * 
     * Usage: POST /api/admin/payment-fix/trigger/{bookingId}
     */
    @PostMapping("/trigger/{bookingId}")
    public ResponseEntity<Map<String, Object>> triggerPaymentUpdate(@PathVariable Integer bookingId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Get booking before
            BookingEntity before = bookingRepository.findById(bookingId).orElse(null);
            if (before == null) {
                result.put("error", "Booking not found");
                return ResponseEntity.status(404).body(result);
            }

            result.put("before", Map.of(
                "status", before.getStatus(),
                "paymentState", before.getPaymentState()
            ));

            // Get current paid amount
            BigDecimal paid = paymentRepository.sumPaidByBooking(bookingId);
            result.put("totalPaid", paid != null ? paid.toString() : "0");
            result.put("totalPrice", before.getTotalPrice().toString());
            result.put("depositAmount", before.getDepositAmount().toString());

            // Trigger update
            bookingService.onPaymentCaptured(bookingId);

            // Get booking after
            BookingEntity after = bookingRepository.findById(bookingId).orElse(null);
            result.put("after", Map.of(
                "status", after.getStatus(),
                "paymentState", after.getPaymentState(),
                "checkInCode", after.getCheckInCode() != null ? after.getCheckInCode() : "N/A"
            ));

            result.put("success", true);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("error", e.getMessage());
            result.put("success", false);
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * Get diagnostic info for a booking
     * 
     * Usage: GET /api/admin/payment-fix/diagnose/{bookingId}
     */
    @GetMapping("/diagnose/{bookingId}")
    public ResponseEntity<Map<String, Object>> diagnoseBooking(@PathVariable Integer bookingId) {
        Map<String, Object> result = new HashMap<>();

        try {
            BookingEntity booking = bookingRepository.findById(bookingId).orElse(null);
            if (booking == null) {
                result.put("error", "Booking not found");
                return ResponseEntity.status(404).body(result);
            }

            result.put("bookingId", bookingId);
            result.put("status", booking.getStatus());
            result.put("paymentState", booking.getPaymentState());
            result.put("totalPrice", booking.getTotalPrice().toString());
            result.put("depositAmount", booking.getDepositAmount().toString());
            result.put("checkInCode", booking.getCheckInCode());

            // Get payments
            var payments = paymentRepository.findAll().stream()
                .filter(p -> p.getBooking() != null && p.getBooking().getId().equals(bookingId))
                .map(p -> Map.of(
                    "id", p.getId(),
                    "amount", p.getAmount().toString(),
                    "status", p.getStatus(),
                    "date", p.getPaymentDate().toString()
                ))
                .toList();
            result.put("payments", payments);

            BigDecimal totalPaid = paymentRepository.sumPaidByBooking(bookingId);
            result.put("totalPaid", totalPaid != null ? totalPaid.toString() : "0");

            // Diagnosis
            List<String> issues = new ArrayList<>();
            if (totalPaid != null && totalPaid.compareTo(BigDecimal.ZERO) > 0) {
                if (totalPaid.compareTo(booking.getDepositAmount()) >= 0 
                    && "unpaid".equals(booking.getPaymentState())) {
                    issues.add("⚠️ Đã thanh toán >= deposit nhưng payment_state vẫn 'unpaid'");
                }
                if (totalPaid.compareTo(booking.getDepositAmount()) >= 0 
                    && "pending".equals(booking.getStatus())) {
                    issues.add("⚠️ Đã thanh toán >= deposit nhưng status vẫn 'pending'");
                }
                if (totalPaid.compareTo(booking.getTotalPrice()) >= 0 
                    && !"paid_in_full".equals(booking.getPaymentState())) {
                    issues.add("⚠️ Đã thanh toán full nhưng payment_state không phải 'paid_in_full'");
                }
            }

            if (issues.isEmpty()) {
                result.put("diagnosis", "✅ Không phát hiện vấn đề");
            } else {
                result.put("diagnosis", String.join(" | ", issues));
                result.put("suggestedAction", "POST /api/admin/payment-fix/trigger/" + bookingId);
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("error", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * List all bookings with payment issues
     * 
     * Usage: GET /api/admin/payment-fix/list-issues
     */
    @GetMapping("/list-issues")
    public ResponseEntity<List<Map<String, Object>>> listIssues() {
        try {
            List<Map<String, Object>> issues = new ArrayList<>();
            
            List<BookingEntity> allBookings = bookingRepository.findAll();
            
            for (BookingEntity b : allBookings) {
                BigDecimal paid = paymentRepository.sumPaidByBooking(b.getId());
                if (paid == null || paid.compareTo(BigDecimal.ZERO) == 0) continue;

                boolean hasIssue = false;
                List<String> problems = new ArrayList<>();

                if (b.getDepositAmount() != null && paid.compareTo(b.getDepositAmount()) >= 0) {
                    if ("unpaid".equals(b.getPaymentState())) {
                        problems.add("payment_state=unpaid but paid>=deposit");
                        hasIssue = true;
                    }
                    if ("pending".equals(b.getStatus())) {
                        problems.add("status=pending but paid>=deposit");
                        hasIssue = true;
                    }
                }

                if (b.getTotalPrice() != null && paid.compareTo(b.getTotalPrice()) >= 0) {
                    if (!"paid_in_full".equals(b.getPaymentState())) {
                        problems.add("payment_state!=paid_in_full but paid>=total");
                        hasIssue = true;
                    }
                }

                if (hasIssue) {
                    Map<String, Object> issue = new HashMap<>();
                    issue.put("bookingId", b.getId());
                    issue.put("status", b.getStatus());
                    issue.put("paymentState", b.getPaymentState());
                    issue.put("totalPaid", paid.toString());
                    issue.put("depositAmount", b.getDepositAmount().toString());
                    issue.put("totalPrice", b.getTotalPrice().toString());
                    issue.put("issues", problems);
                    issue.put("fixUrl", "/api/admin/payment-fix/trigger/" + b.getId());
                    issues.add(issue);
                }
            }

            return ResponseEntity.ok(issues);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(List.of(error));
        }
    }
}

