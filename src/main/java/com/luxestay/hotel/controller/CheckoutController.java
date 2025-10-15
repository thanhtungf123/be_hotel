package com.luxestay.hotel.controller;


import com.luxestay.hotel.model.entity.BookingEntity;
import com.luxestay.hotel.repository.BookingRepository;
import com.luxestay.hotel.response.ApiResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;

import java.math.BigDecimal;
import java.util.Optional;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {
    private final PayOS payOS;
    private final BookingRepository bookingRepository;

    @Autowired
    public CheckoutController(PayOS payOS, BookingRepository bookingRepository) {
        this.payOS = payOS;
        this.bookingRepository = bookingRepository;
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

            String returnUrl = "http://localhost:3000/payment/success?bookingId=" + booking.getId();
            String cancelUrl = "http://localhost:3000/payment/cancel?bookingId=" + booking.getId();

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
}