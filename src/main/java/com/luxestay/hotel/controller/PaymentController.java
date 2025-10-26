package com.luxestay.hotel.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.luxestay.hotel.model.Payment;
import com.luxestay.hotel.model.entity.BookingEntity;
import com.luxestay.hotel.repository.BookingRepository;
import com.luxestay.hotel.repository.PaymentRepository;
import com.luxestay.hotel.response.ApiResponse;
import com.luxestay.hotel.service.BookingService; // Đã import
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.payos.PayOS;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
// no direct WebhookData type usage; we parse generically for SDK compatibility

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PayOS payOS;
    private final BookingService bookingService; // Chính xác
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    @Autowired
    public PaymentController(PayOS payOS,
                             PaymentRepository paymentRepository,
                             BookingService bookingService,
                             BookingRepository bookingRepository) { // Chính xác
        this.payOS = payOS;
        this.bookingService = bookingService;
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
    }


    @PostMapping(path = "/payos_transfer_handler")
    public ApiResponse<Object> payosTransferHandler(@RequestBody Object body)
            throws JsonProcessingException, IllegalArgumentException {
        try {
            Object data = payOS.webhooks().verify(body);
            System.out.println("Webhook data received: " + data);

            ObjectMapper mapper = new ObjectMapper();
            var map = mapper.convertValue(data, new TypeReference<java.util.Map<String,Object>>(){});
            String code = String.valueOf(map.getOrDefault("code", ""));
            String idStr = String.valueOf(map.getOrDefault("id", ""));
            long orderCode = Long.parseLong(String.valueOf(map.getOrDefault("orderCode", "0")));

            if ("00".equals(code)) {

                try {
                    // 1) Persist payment record
                    BookingEntity booking = bookingRepository.findById((int) orderCode)
                            .orElse(null);
                    if (booking != null) {
                        BigDecimal amount = booking.getTotalPrice();
                        Payment payment = Payment.builder()
                                .booking(booking)
                                .amount(amount)
                                .paymentMethod("PayOS")
                                .paymentDate(LocalDateTime.now())
                                .status("completed")
                                .transactionId(idStr)
                                .build();
                        paymentRepository.save(payment);
                    }

                    // 2) Confirm booking status and room state
                    bookingService.confirmBookingPayment((int) orderCode);
                    bookingService.onPaymentCaptured((int) orderCode);
                    System.out.println("Successfully processed payment confirmation for ID: " + orderCode);

                } catch (Exception e) {
                    System.err.println("Error processing payment confirmation logic: " + e.getMessage());
                }
                // ========================

            } else {
                System.out.println("Payment failed or not yet completed for order: " + orderCode + " with code: " + code);
            }

            return ApiResponse.success("Webhook delivered", data);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error(e.getMessage());
        }
    }
}