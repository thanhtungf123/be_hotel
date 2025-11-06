package com.luxestay.hotel.controller;

import com.luxestay.hotel.dto.booking.BookingSummary;
import com.luxestay.hotel.dto.PagedResponse;
import com.luxestay.hotel.model.entity.BookingEntity;
import com.luxestay.hotel.repository.BookingRepository;
import com.luxestay.hotel.repository.PaymentRepository;
import com.luxestay.hotel.response.ApiResponse;
import com.luxestay.hotel.service.AuthService;
import com.luxestay.hotel.service.BookingQueryService;
import com.luxestay.hotel.service.EmailService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingQueryController {

    private final AuthService authService;
    private final BookingQueryService bookingQueryService;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final EntityManager entityManager;
    private final EmailService emailService;

    // GET /api/bookings/my?status=&page=&size=
    // Lấy lịch sử đặt phòng của KH đang đăng nhập (có lọc status)
    @GetMapping("/my")
    public ResponseEntity<PagedResponse<BookingSummary>> myBookings(
            @RequestHeader("X-Auth-Token") String token,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        Integer accountId = authService.verify(token)
                .orElseThrow(() -> new IllegalArgumentException("Bạn cần đăng nhập"));
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by("createdAt").descending());
        return ResponseEntity.ok(bookingQueryService.listMine(accountId, status, pageable));
    }

    // Lấy danh sách yêu cầu hủy đặt phòng của Admin/Staff
    // Bao gồm cả cancel_requested và cancelled (có refund info)
    @GetMapping("/cancel-requests")
    public ResponseEntity<PagedResponse<BookingSummary>> listCancelRequests(
            @RequestHeader("X-Auth-Token") String token,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "refundPending", required = false) Boolean refundPending) {
        var acc = authService.requireAccount(token);
        String role = acc.getRole() != null ? acc.getRole().getName() : "";
        if (!"admin".equalsIgnoreCase(role) && !"staff".equalsIgnoreCase(role)) {
            return ResponseEntity.status(403)
                    .body(new PagedResponse<>(java.util.List.of(), 0, page, size));
        }
        
        // Tạo Pageable không có sort vì @Query đã có ORDER BY
        var pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        
        // Nếu refundPending=true, chỉ trả về các đơn cancelled có refund info nhưng chưa completed
        if (Boolean.TRUE.equals(refundPending)) {
            var refundPendingPage = bookingRepository.findRefundPendingBookings(pageable);
            var svc = (com.luxestay.hotel.service.impl.BookingQueryServiceImpl) bookingQueryService;
            var items = refundPendingPage.getContent().stream().map(svc::summaryOf).toList();
            
            return ResponseEntity.ok(new PagedResponse<>(
                items, 
                refundPendingPage.getTotalElements(), 
                refundPendingPage.getNumber(), 
                refundPendingPage.getSize()
            ));
        }
        
        // Mặc định: trả về cả cancel_requested và cancelled
        var cancelPage = bookingRepository.findCancelRequestsAndCancelled(pageable);
        var svc = (com.luxestay.hotel.service.impl.BookingQueryServiceImpl) bookingQueryService;
        var items = cancelPage.getContent().stream().map(svc::summaryOf).toList();
        
        return ResponseEntity.ok(new PagedResponse<>(
            items,
            cancelPage.getTotalElements(),
            cancelPage.getNumber(),
            cancelPage.getSize()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingSummary> getOne(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable("id") Integer id) {
        Integer accountId = authService.verify(token)
                .orElseThrow(() -> new IllegalArgumentException("Bạn cần đăng nhập"));

        // ✅ FIX: Dùng bookingRepository đã inject thay vì cast sai
        var be = bookingRepository.findByIdWithServices(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy"));
        
        if (be.getAccount() == null || !be.getAccount().getId().equals(accountId))
            return ResponseEntity.status(403).build();

        // Dùng helper summaryOf trong service impl
        var svc = (com.luxestay.hotel.service.impl.BookingQueryServiceImpl) bookingQueryService;
        return ResponseEntity.ok(svc.summaryOf(be));
    }

    /**
     * Manually sync payment status for a booking
     * Useful when webhook hasn't been called or is delayed
     * 
     * POST /api/bookings/{id}/sync-payment-status
     */
    @PostMapping("/{id}/sync-payment-status")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> syncPaymentStatus(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable("id") Integer id) {
        
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║        SYNC PAYMENT STATUS ENDPOINT CALLED                     ║");
        System.out.println("╠═══════════════════════════════════════════════════════════════╣");
        System.out.println("  Booking ID: " + id);
        
        try {
            Integer accountId = authService.verify(token)
                    .orElseThrow(() -> new IllegalArgumentException("Bạn cần đăng nhập"));
            System.out.println("  Account ID: " + accountId);

            var be = bookingRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy booking"));

            System.out.println("  Found booking:");
            System.out.println("    Status: " + be.getStatus());
            System.out.println("    Payment State: " + be.getPaymentState());
            System.out.println("    Check-in Code: " + be.getCheckInCode());

            if (be.getAccount() == null || !be.getAccount().getId().equals(accountId)) {
                System.out.println("  ❌ Permission denied: Account ID mismatch");
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("Bạn không có quyền truy cập booking này"));
            }

            // Call internal sync method
            syncPaymentStatusInternal(id);
            
            // Reload booking to get updated state
            be = bookingRepository.findById(id).orElse(null);
            if (be == null) {
                System.out.println("  ❌ Booking not found after sync");
                return ResponseEntity.status(404).body(ApiResponse.error("Booking not found"));
            }

            System.out.println("  ✅ Sync completed:");
            System.out.println("    Final Status: " + be.getStatus());
            System.out.println("    Final Payment State: " + be.getPaymentState());
            System.out.println("    Final Check-in Code: " + be.getCheckInCode());
            System.out.println("╚═══════════════════════════════════════════════════════════════╝");

            // Use HashMap instead of Map.of() to handle null values
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("bookingId", id);
            result.put("status", be.getStatus() != null ? be.getStatus() : "");
            result.put("paymentState", be.getPaymentState() != null ? be.getPaymentState() : "");
            result.put("checkInCode", be.getCheckInCode() != null ? be.getCheckInCode() : "");

            return ResponseEntity.ok(ApiResponse.success("Payment status updated", result));
        } catch (Exception e) {
            System.err.println("╔═══════════════════════════════════════════════════════════════╗");
            System.err.println("║                    ❌ SYNC ENDPOINT ERROR ❌                   ║");
            System.err.println("╠═══════════════════════════════════════════════════════════════╣");
            System.err.println("  Booking ID: " + id);
            System.err.println("  Error: " + e.getMessage());
            System.err.println("  Stack trace:");
            e.printStackTrace();
            System.err.println("╚═══════════════════════════════════════════════════════════════╝");
            
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to sync payment status: " + e.getMessage()));
        }
    }

    @Transactional
    private void syncPaymentStatusInternal(int bookingId) {
        System.out.println("  [syncPaymentStatusInternal] Starting for booking ID: " + bookingId);
        
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        System.out.println("  [syncPaymentStatusInternal] Current state:");
        System.out.println("    Status: " + booking.getStatus());
        System.out.println("    Payment State: " + booking.getPaymentState());
        System.out.println("    Check-in Code: " + booking.getCheckInCode());

        BigDecimal totalPaid = paymentRepository.sumPaidByBooking(bookingId);
        if (totalPaid == null) totalPaid = BigDecimal.ZERO;
        
        System.out.println("  [syncPaymentStatusInternal] Total Paid: " + totalPaid);
        System.out.println("  [syncPaymentStatusInternal] Deposit Amount: " + booking.getDepositAmount());
        System.out.println("  [syncPaymentStatusInternal] Total Price: " + booking.getTotalPrice());

        String paymentState = "unpaid";
        if (booking.getDepositAmount() != null && totalPaid.compareTo(booking.getDepositAmount()) >= 0) {
            if (booking.getTotalPrice() != null && totalPaid.compareTo(booking.getTotalPrice()) >= 0) {
                paymentState = "paid_in_full";
                System.out.println("  [syncPaymentStatusInternal] Determined: paid_in_full");
            } else {
                paymentState = "deposit_paid";
                System.out.println("  [syncPaymentStatusInternal] Determined: deposit_paid");
            }
        } else {
            System.out.println("  [syncPaymentStatusInternal] Determined: unpaid");
        }

        booking.setPaymentState(paymentState);
        if ("deposit_paid".equals(paymentState) || "paid_in_full".equals(paymentState)) {
            booking.setStatus("confirmed");
            System.out.println("  [syncPaymentStatusInternal] Setting status to: confirmed");
        }

        bookingRepository.save(booking);
        entityManager.flush();
        System.out.println("  [syncPaymentStatusInternal] Booking saved and flushed");

        // Re-fetch booking to ensure we have latest state
        booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found after save: " + bookingId));

        // Generate check-in code if needed
        if (("deposit_paid".equals(paymentState) || "paid_in_full".equals(paymentState))
                && (booking.getCheckInCode() == null || booking.getCheckInCode().trim().isEmpty())) {
            System.out.println("  [syncPaymentStatusInternal] Generating check-in code...");
            String code = generateCheckInCode(6);
            booking.setCheckInCode(code);
            bookingRepository.save(booking);
            entityManager.flush();
            
            // Re-fetch to verify check-in code was saved
            booking = bookingRepository.findById(bookingId).orElse(null);
            if (booking != null && code.equals(booking.getCheckInCode())) {
                System.out.println("  [syncPaymentStatusInternal] ✅ Generated check-in code: " + code);
            } else {
                System.err.println("  [syncPaymentStatusInternal] ⚠️ WARNING: Check-in code not saved correctly!");
                System.err.println("    Expected: " + code);
                System.err.println("    Actual: " + (booking != null ? booking.getCheckInCode() : "null"));
            }
            
            // Send email notification
            try {
                String email = booking.getAccount() != null ? booking.getAccount().getEmail() : null;
                String customerName = booking.getCustomerDetails() != null 
                    ? booking.getCustomerDetails().getFullName() 
                    : (booking.getAccount() != null ? booking.getAccount().getFullName() : "Quý khách");
                String roomName = booking.getRoom() != null ? booking.getRoom().getRoomName() : "";
                String in = booking.getCheckIn() != null ? booking.getCheckIn().toString() : "";
                String out = booking.getCheckOut() != null ? booking.getCheckOut().toString() : "";
                if (email != null && !email.isBlank()) {
                    emailService.sendBookingConfirmation(email, customerName, roomName, in, out, paymentState, code);
                    System.out.println("  [syncPaymentStatusInternal] ✅ Sent email to: " + email);
                }
            } catch (Exception e) {
                System.err.println("  [syncPaymentStatusInternal] ⚠️ Failed to send email: " + e.getMessage());
                e.printStackTrace();
                // Don't throw - email failure shouldn't break the sync
            }
        } else {
            System.out.println("  [syncPaymentStatusInternal] Check-in code already exists or not needed");
        }
        
        System.out.println("  [syncPaymentStatusInternal] Completed successfully");
    }

    private String generateCheckInCode(int length) {
        final String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        java.util.Random rnd = new java.util.Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }
    
    /**
     * Generate check-in code for all confirmed bookings missing check-in code
     * POST /api/bookings/generate-checkin-codes
     */
    @PostMapping("/generate-checkin-codes")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateCheckInCodes(
            @RequestHeader("X-Auth-Token") String token) {
        var acc = authService.requireAccount(token);
        String role = acc.getRole() != null ? acc.getRole().getName() : "";
        if (!"admin".equalsIgnoreCase(role) && !"staff".equalsIgnoreCase(role)) {
            return ResponseEntity.status(403).body(ApiResponse.error("Chỉ admin/staff mới được phép"));
        }

        try {
            // Find all confirmed bookings without check-in code
            var allBookings = bookingRepository.findAll();
            var bookings = allBookings.stream()
                    .filter(b -> ("confirmed".equalsIgnoreCase(b.getStatus()) 
                            || "deposit_paid".equalsIgnoreCase(b.getPaymentState()) 
                            || "paid_in_full".equalsIgnoreCase(b.getPaymentState()))
                            && (b.getCheckInCode() == null || b.getCheckInCode().isBlank()))
                    .toList();
            
            int count = 0;
            for (var booking : bookings) {
                String code = generateCheckInCode(6);
                booking.setCheckInCode(code);
                bookingRepository.save(booking);
                entityManager.flush();
                count++;
                
                // Send email if needed
                try {
                    String email = booking.getAccount() != null ? booking.getAccount().getEmail() : null;
                    if (email != null && !email.isBlank()) {
                        String customerName = booking.getCustomerDetails() != null 
                            ? booking.getCustomerDetails().getFullName() 
                            : (booking.getAccount() != null ? booking.getAccount().getFullName() : "Quý khách");
                        String roomName = booking.getRoom() != null ? booking.getRoom().getRoomName() : "";
                        String in = booking.getCheckIn() != null ? booking.getCheckIn().toString() : "";
                        String out = booking.getCheckOut() != null ? booking.getCheckOut().toString() : "";
                        String paymentState = booking.getPaymentState() != null ? booking.getPaymentState() : "";
                        
                        emailService.sendBookingConfirmation(email, customerName, roomName, in, out, paymentState, code);
                        System.out.println("✅ Sent check-in code email to: " + email + " for booking #" + booking.getId());
                    }
                } catch (Exception e) {
                    System.err.println("⚠️ Failed to send email for booking " + booking.getId() + ": " + e.getMessage());
                }
            }
            
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("updated", count);
            result.put("message", "Generated check-in codes for " + count + " bookings");
            
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }
}
