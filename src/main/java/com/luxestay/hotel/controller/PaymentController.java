package com.luxestay.hotel.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
// Bỏ các import không dùng đến BookingEntity, Optional, BookingRepository
import com.luxestay.hotel.repository.PaymentRepository;
import com.luxestay.hotel.response.ApiResponse;
import com.luxestay.hotel.service.BookingService; // Đã import
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.payos.PayOS;
import vn.payos.model.webhooks.WebhookData;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PayOS payOS;
    private final BookingService bookingService; // Chính xác

    @Autowired
    public PaymentController(PayOS payOS,
                             PaymentRepository paymentRepository,
                             BookingService bookingService) { // Chính xác
        this.payOS = payOS;
        this.bookingService = bookingService;
    }


    @PostMapping(path = "/payos_transfer_handler")
    public ApiResponse<WebhookData> payosTransferHandler(@RequestBody Object body)
            throws JsonProcessingException, IllegalArgumentException {
        try {
            WebhookData data = payOS.webhooks().verify(body);
            System.out.println("Webhook data received: " + data);

            if ("00".equals(data.getCode())) {
                long orderCode = data.getOrderCode();

                try {
                    bookingService.confirmBookingPayment((int) orderCode);
                    System.out.println("Successfully processed payment confirmation for ID: " + orderCode);

                } catch (Exception e) {
                    System.err.println("Error processing payment confirmation logic: " + e.getMessage());
                }
                // ========================

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