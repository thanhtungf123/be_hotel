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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

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
    public ResponseEntity<ApiResponse<CreatePaymentLinkResponse>> createPaymentLink(
            @PathVariable("bookingId") Integer bookingId,
            @RequestParam(name = "purpose", defaultValue = "full") String purpose
    ) {
        try {
            Optional<BookingEntity> bookingOpt = bookingRepository.findById(bookingId);
            if (bookingOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Booking not found with ID: " + bookingId));
            }
            BookingEntity booking = bookingOpt.get();

            // Số tiền theo mục đích
            BigDecimal paid = paymentRepository.sumPaidByBooking(bookingId);
            if (paid == null) paid = BigDecimal.ZERO;

            BigDecimal amountDecimal;
            switch (purpose.toLowerCase()) {
                case "deposit" -> amountDecimal = booking.getDepositAmount();
                case "balance" -> amountDecimal = booking.getTotalPrice().subtract(paid).max(BigDecimal.ZERO);
                default -> amountDecimal = booking.getTotalPrice();
            }

            if (amountDecimal == null || amountDecimal.compareTo(BigDecimal.ZERO) <= 0)
                return ResponseEntity.badRequest().body(ApiResponse.error("Invalid amount."));

            long orderCode = booking.getId();
            long amount = amountDecimal.longValue();
            String description = "Booking #" + booking.getId() + " " + purpose;

            String beBase = "http://localhost:8080";
            String returnUrl = beBase + "/api/checkout/return?bookingId=" + booking.getId();
            String cancelUrl = beBase + "/api/checkout/cancel?bookingId=" + booking.getId();

            PaymentLinkItem item = PaymentLinkItem.builder()
                    .name("Payment " + purpose)
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
                BookingEntity booking = bookingRepository.findById(bookingId).orElse(null);
                if (booking != null) {
                    paymentRepository.save(Payment.builder()
                            .booking(booking)
                            .amount(booking.getTotalPrice()) // lưu fallback full; thực tế PayOS có amount, nếu cần parse thêm
                            .paymentMethod("PayOS")
                            .paymentDate(java.time.LocalDateTime.now())
                            .status("completed")
                            .transactionId(id)
                            .build());
                    // NEW:
                    bookingService.onPaymentCaptured(bookingId);
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

    @PostMapping(path = "/payos_transfer_handler")
    public ApiResponse<Object> payosTransferHandler(@RequestBody Object body)
            throws JsonProcessingException, IllegalArgumentException {
        try {
            Object data = payOS.webhooks().verify(body);
            ObjectMapper mapper = new ObjectMapper();
            var map = mapper.convertValue(data, new TypeReference<java.util.Map<String,Object>>(){});
            String code = String.valueOf(map.getOrDefault("code", ""));
            String idStr = String.valueOf(map.getOrDefault("id", ""));
            long orderCode = Long.parseLong(String.valueOf(map.getOrDefault("orderCode", "0")));

            if ("00".equals(code)) {
                try {
                    BookingEntity booking = bookingRepository.findById((int) orderCode).orElse(null);
                    if (booking != null) {
                        BigDecimal amount = booking.getTotalPrice(); // nếu cần lấy exact amount từ webhook, parse thêm
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

                    // ✅ Block ngay sau thanh toán
                    bookingService.onPaymentCaptured((int) orderCode);
                } catch (Exception e) {
                    System.err.println("Error processing payment confirmation logic: " + e.getMessage());
                }
            }
            return ApiResponse.success("Webhook delivered", data);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error(e.getMessage());
        }
    }
}