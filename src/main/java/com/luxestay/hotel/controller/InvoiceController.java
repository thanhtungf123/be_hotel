package com.luxestay.hotel.controller;

import com.luxestay.hotel.dto.invoice.InvoiceDTO;
import com.luxestay.hotel.model.Account;
import com.luxestay.hotel.model.entity.BookingEntity;
import com.luxestay.hotel.repository.BookingRepository;
import com.luxestay.hotel.service.AuthService;
import com.luxestay.hotel.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {
    
    private final AuthService authService;
    private final InvoiceService invoiceService;
    private final BookingRepository bookingRepository;
    
    /**
     * Get invoice for a booking
     * Customer can only view their own invoices
     */
    @GetMapping("/{bookingId}")
    public ResponseEntity<?> getInvoice(
        @RequestHeader("X-Auth-Token") String token,
        @PathVariable Integer bookingId
    ) {
        Account account = authService.requireAccount(token);
        
        // Verify user owns this booking or is staff/admin
        BookingEntity booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy booking"));
        
        String role = account.getRole() != null ? account.getRole().getName() : "";
        boolean isStaffOrAdmin = "admin".equalsIgnoreCase(role) || "staff".equalsIgnoreCase(role);
        boolean ownsBooking = booking.getAccount() != null && booking.getAccount().getId().equals(account.getId());
        
        if (!isStaffOrAdmin && !ownsBooking) {
            return ResponseEntity.status(403).body(Map.of("error", "Không có quyền xem hóa đơn này"));
        }
        
        // Check if booking has been paid
        String paymentState = booking.getPaymentState();
        if (!"deposit_paid".equals(paymentState) && !"paid_in_full".equals(paymentState)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Booking chưa thanh toán"));
        }
        
        try {
            InvoiceDTO invoice = invoiceService.generateInvoice(bookingId);
            return ResponseEntity.ok(invoice);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Send invoice email
     */
    @PostMapping("/{bookingId}/send")
    public ResponseEntity<?> sendInvoiceEmail(
        @RequestHeader("X-Auth-Token") String token,
        @PathVariable Integer bookingId
    ) {
        Account account = authService.requireAccount(token);
        
        // Verify user owns this booking or is staff/admin
        BookingEntity booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy booking"));
        
        String role = account.getRole() != null ? account.getRole().getName() : "";
        boolean isStaffOrAdmin = "admin".equalsIgnoreCase(role) || "staff".equalsIgnoreCase(role);
        boolean ownsBooking = booking.getAccount() != null && booking.getAccount().getId().equals(account.getId());
        
        if (!isStaffOrAdmin && !ownsBooking) {
            return ResponseEntity.status(403).body(Map.of("error", "Không có quyền gửi hóa đơn này"));
        }
        
        try {
            invoiceService.sendInvoiceEmail(bookingId);
            return ResponseEntity.ok(Map.of("message", "Đã gửi hóa đơn qua email"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

