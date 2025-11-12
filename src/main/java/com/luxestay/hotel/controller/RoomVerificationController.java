package com.luxestay.hotel.controller;

import com.luxestay.hotel.dto.booking.RoomVerificationRequest;
import com.luxestay.hotel.model.Account;
import com.luxestay.hotel.model.entity.BookingEntity;
import com.luxestay.hotel.repository.BookingRepository;
import com.luxestay.hotel.repository.PaymentRepository;
import com.luxestay.hotel.service.AuthService;
import com.luxestay.hotel.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/staff/bookings")
@RequiredArgsConstructor
public class RoomVerificationController {
    
    private final AuthService authService;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final EmailService emailService;
    
    /**
     * Staff verifies room status after customer payment
     * POST /api/staff/bookings/{bookingId}/verify-room
     */
    @PostMapping("/{bookingId}/verify-room")
    public ResponseEntity<?> verifyRoom(
        @RequestHeader("X-Auth-Token") String token,
        @PathVariable Integer bookingId,
        @RequestBody RoomVerificationRequest request
    ) {
        // Check staff/admin permission
        Account account = authService.requireAccount(token);
        String role = account.getRole() != null ? account.getRole().getName() : "";
        if (!"staff".equalsIgnoreCase(role) && !"admin".equalsIgnoreCase(role)) {
            return ResponseEntity.status(403).body(Map.of("error", "Không có quyền truy cập"));
        }
        
        // Find booking
        BookingEntity booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy booking"));
        
        // Check if booking is pending verification
        if (!"pending_verification".equals(booking.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Booking không ở trạng thái chờ kiểm tra",
                "currentStatus", booking.getStatus()
            ));
        }
        
        try {
            if (Boolean.TRUE.equals(request.getIsRoomReady())) {
                // ✅ Room is ready - Confirm booking
                booking.setStatus("confirmed");
                bookingRepository.save(booking);
                
                // Send confirmation email
                String email = booking.getAccount() != null ? booking.getAccount().getEmail() : null;
                String customerName = booking.getCustomerDetails() != null 
                    ? booking.getCustomerDetails().getFullName() 
                    : (booking.getAccount() != null ? booking.getAccount().getFullName() : "Quý khách");
                String roomName = booking.getRoom() != null ? booking.getRoom().getRoomName() : "";
                String checkIn = booking.getCheckIn() != null ? booking.getCheckIn().toString() : "";
                String checkOut = booking.getCheckOut() != null ? booking.getCheckOut().toString() : "";
                String checkInCode = booking.getCheckInCode() != null ? booking.getCheckInCode() : "";
                
                if (email != null && !email.isBlank()) {
                    emailService.sendRoomConfirmedEmail(email, customerName, roomName, checkIn, checkOut, checkInCode);
                }
                
                return ResponseEntity.ok(Map.of(
                    "message", "Xác nhận phòng thành công",
                    "bookingId", bookingId,
                    "status", "confirmed"
                ));
                
            } else {
                // ❌ Room has issues - Cancel booking and initiate refund
                booking.setStatus("cancelled");
                booking.setCancelReason("Phòng gặp vấn đề: " + (request.getIssueDescription() != null ? request.getIssueDescription() : ""));
                
                // Set refund to 100% of amount paid
                BigDecimal totalPaid = paymentRepository.sumPaidByBooking(bookingId);
                if (totalPaid == null) totalPaid = BigDecimal.ZERO;
                booking.setRefundAmount(totalPaid);
                booking.setRefundPercent(100);
                
                bookingRepository.save(booking);
                
                // Send issue email
                String email = booking.getAccount() != null ? booking.getAccount().getEmail() : null;
                String customerName = booking.getCustomerDetails() != null 
                    ? booking.getCustomerDetails().getFullName() 
                    : (booking.getAccount() != null ? booking.getAccount().getFullName() : "Quý khách");
                String roomName = booking.getRoom() != null ? booking.getRoom().getRoomName() : "";
                String issueDesc = request.getIssueDescription() != null && !request.getIssueDescription().isBlank()
                    ? request.getIssueDescription()
                    : "Phòng không sẵn sàng phục vụ";
                
                if (email != null && !email.isBlank()) {
                    emailService.sendRoomIssueEmail(email, customerName, bookingId, roomName, issueDesc);
                }
                
                return ResponseEntity.ok(Map.of(
                    "message", "Đã hủy booking và gửi email thông báo cho khách hàng",
                    "bookingId", bookingId,
                    "status", "cancelled",
                    "refundAmount", totalPaid.intValue()
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

