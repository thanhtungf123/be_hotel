package com.luxestay.hotel.controller;


import com.luxestay.hotel.model.entity.BookingEntity;
import com.luxestay.hotel.model.Payment;
import com.luxestay.hotel.repository.BookingRepository;
import com.luxestay.hotel.repository.PaymentRepository;
import com.luxestay.hotel.response.ApiResponse;
import com.luxestay.hotel.service.BookingService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.payos.PayOS;

import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;


import java.math.BigDecimal;
import java.util.Optional;
// Thêm import này vào đầu file

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {
    private final PayOS payOS;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final BookingService bookingService;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Autowired
    public CheckoutController(PayOS payOS, BookingRepository bookingRepository,
                              PaymentRepository paymentRepository, BookingService bookingService) {
        this.payOS = payOS;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.bookingService = bookingService;
    }

    @PostMapping("/{bookingId}/create-payment-link")
    public ResponseEntity<ApiResponse<CreatePaymentLinkResponse>> createPaymentLink(@PathVariable("bookingId") Integer bookingId) {
        try {
            Optional<BookingEntity> bookingOpt = bookingRepository.findById(bookingId);
            if (bookingOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Booking not found with ID: " + bookingId));
            }
            BookingEntity booking = bookingOpt.get();

            long orderCode = booking.getId();
            BigDecimal amountDecimal = booking.getTotalPrice();
            if (amountDecimal == null || amountDecimal.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Invalid booking price."));
            }
            long amount = amountDecimal.longValue();

            String description = "Thanh toan dat phong" + booking.getId();

            // Prefer returning to backend so we can finalize then redirect FE
            String beBase = "http://localhost:8080"; // could be externalized later
            String returnUrl = beBase + "/api/checkout/return?bookingId=" + booking.getId();
            String cancelUrl = beBase + "/api/checkout/cancel?bookingId=" + booking.getId();

            PaymentLinkItem item = PaymentLinkItem.builder()
                    .name("Thanh toan dat phong khach san")
                    .quantity(1)
                    .price(amount)
                    .build();

            CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                    .orderCode(orderCode)
                    .amount(amount)
                    .description(description)
                    .returnUrl(returnUrl)
                    .cancelUrl(cancelUrl)
                    .item(item)
                    .build();

            CreatePaymentLinkResponse payosResponse = payOS.paymentRequests().create(paymentData);
            return ResponseEntity.ok(ApiResponse.success(payosResponse));

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Failed to create payment link: " + e.getMessage()));
        }
    }


    @GetMapping("/return")
    public ResponseEntity<Void> handleReturn(
            @RequestParam Integer bookingId,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String id,
            @RequestParam(required = false) Long orderCode
    ) {
        try {
            boolean success = "00".equalsIgnoreCase(String.valueOf(code))
                    || "PAID".equalsIgnoreCase(String.valueOf(status));
            if (success) {
                // Fallback persistence in case webhook is not reachable in dev
                BookingEntity booking = bookingRepository.findById(bookingId).orElse(null);
                if (booking != null) {
                    // Save payment row if not already saved (naive insert; duplicates unlikely in dev)
                    paymentRepository.save(Payment.builder()
                            .booking(booking)
                            .amount(booking.getTotalPrice())
                            .paymentMethod("PayOS")
                            .paymentDate(java.time.LocalDateTime.now())
                            .status("completed")
                            .transactionId(id)
                            .build());
                    // Confirm booking
                    bookingService.confirmBookingPayment(bookingId);
                }
            }
        } catch (Exception ignore) {}

        String feBase = frontendBaseUrl != null ? frontendBaseUrl : "http://localhost:5173";
        String location = feBase.replaceAll("/+$", "") + "/payment/success?bookingId=" + bookingId;
        return ResponseEntity.status(302).header(HttpHeaders.LOCATION, location).build();
    }

    @GetMapping("/cancel")
    public ResponseEntity<Void> handleCancel(@RequestParam Integer bookingId) {
        String feBase = frontendBaseUrl != null ? frontendBaseUrl : "http://localhost:5173";
        String location = feBase.replaceAll("/+$", "") + "/payment/cancel?bookingId=" + bookingId;
        return ResponseEntity.status(302).header(HttpHeaders.LOCATION, location).build();
    }
}