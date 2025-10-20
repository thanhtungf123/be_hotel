package com.luxestay.hotel.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.luxestay.hotel.model.entity.BookingEntity;
import com.luxestay.hotel.model.Payment;
import com.luxestay.hotel.repository.BookingRepository;
import com.luxestay.hotel.repository.PaymentRepository;
import com.luxestay.hotel.response.ApiResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.webhooks.WebhookData;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PayOS payOS;
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    @Autowired
    public PaymentController(PayOS payOS, PaymentRepository paymentRepository, BookingRepository bookingRepository) {
        this.payOS = payOS;
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
    }


    @PostMapping(path = "/payos_transfer_handler")
    public ApiResponse<WebhookData> payosTransferHandler(@RequestBody Object body)
            throws JsonProcessingException, IllegalArgumentException {
        try {
            WebhookData data = payOS.webhooks().verify(body);
            System.out.println("Webhook data received: " + data);

            if ("00".equals(data.getCode())) {
                long orderCode = data.getOrderCode();

                Optional<BookingEntity> bookingOpt = bookingRepository.findById((int) orderCode);

                if (bookingOpt.isPresent()) {
                    BookingEntity booking = bookingOpt.get();

                    if ("pending".equalsIgnoreCase(booking.getStatus())) {
                        booking.setStatus("confirmed");
                        bookingRepository.save(booking);
                        System.out.println("Successfully updated booking status for ID: " + orderCode);
                    } else {
                        System.out.println("Booking " + orderCode + " status is already: " + booking.getStatus());
                    }
                } else {
                    System.err.println("Webhook warning: Booking not found with ID: " + orderCode);
                }
            } else {
                System.out.println("Payment failed or not yet completed for order: " + data.getOrderCode() + " with code: " + data.getCode());
            }

            return ApiResponse.success("Webhook delivered", data);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error(e.getMessage());
        }
    }
}