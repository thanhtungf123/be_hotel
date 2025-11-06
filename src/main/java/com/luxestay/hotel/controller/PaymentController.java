package com.luxestay.hotel.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.luxestay.hotel.model.Payment;
import com.luxestay.hotel.model.entity.BookingEntity;
import com.luxestay.hotel.repository.BookingRepository;
import com.luxestay.hotel.repository.PaymentRepository;
import com.luxestay.hotel.response.ApiResponse;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;
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
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final EntityManager entityManager;

    @Autowired
    public PaymentController(PayOS payOS,
                             PaymentRepository paymentRepository,
                             BookingRepository bookingRepository,
                             EntityManager entityManager) {
        this.payOS = payOS;
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.entityManager = entityManager;
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

            // Try to extract paid amount from webhook payload
            java.math.BigDecimal paidAmount = null;
            try {
                Object amtObj = map.get("amount");
                if (amtObj == null && map.get("data") instanceof java.util.Map<?,?> dataMap) {
                    Object innerAmt = ((java.util.Map<?,?>) dataMap).get("amount");
                    if (innerAmt == null && ((java.util.Map<?,?>) dataMap).get("data") instanceof java.util.Map<?,?> deeper) {
                        innerAmt = deeper.get("amount");
                    }
                    amtObj = innerAmt;
                }
                if (amtObj != null) {
                    paidAmount = new java.math.BigDecimal(String.valueOf(amtObj));
                }
            } catch (Exception ignore) {}

            if ("00".equals(code)) {
                System.out.println("╔═══════════════════════════════════════════════════════════════╗");
                System.out.println("║        PAYMENT WEBHOOK RECEIVED - CODE 00 (SUCCESS)          ║");
                System.out.println("╠═══════════════════════════════════════════════════════════════╣");
                System.out.println("  Booking ID: " + orderCode);
                System.out.println("  Transaction ID: " + idStr);
                System.out.println("  Paid Amount from webhook: " + paidAmount);

                try {
                    processPaymentWebhook((int) orderCode, idStr, paidAmount);
                    System.out.println("╚═══════════════════════════════════════════════════════════════╝");

                } catch (Exception e) {
                    System.err.println("╔═══════════════════════════════════════════════════════════════╗");
                    System.err.println("║                    ❌ WEBHOOK ERROR ❌                        ║");
                    System.err.println("╠═══════════════════════════════════════════════════════════════╣");
                    System.err.println("  Booking ID: " + orderCode);
                    System.err.println("  Error: " + e.getMessage());
                    System.err.println("  Stack trace:");
                    e.printStackTrace();
                    System.err.println("╚═══════════════════════════════════════════════════════════════╝");
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

    @Transactional
    private void processPaymentWebhook(int bookingId, String transactionId, BigDecimal paidAmount) {
        // 1) Find booking
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        
        System.out.println("  ✅ Found booking in database");
        System.out.println("     Current Status: " + booking.getStatus());
        System.out.println("     Current Payment State: " + booking.getPaymentState());
        System.out.println("     Total Price: " + booking.getTotalPrice());
        System.out.println("     Deposit Amount: " + booking.getDepositAmount());
        
        // 2) Determine payment amount
        BigDecimal amount = paidAmount != null ? paidAmount : booking.getTotalPrice();
        System.out.println("     Amount to save: " + amount);
        
        // 3) Check if payment already exists (avoid duplicates)
        boolean paymentExists = paymentRepository.findAll().stream()
            .anyMatch(p -> p.getBooking() != null 
                    && p.getBooking().getId().equals(bookingId)
                    && transactionId.equals(p.getTransactionId())
                    && "completed".equalsIgnoreCase(p.getStatus()));
        
        if (paymentExists) {
            System.out.println("  ⚠️ Payment already exists for transaction: " + transactionId);
        } else {
            // 4) Save payment record
            Payment payment = Payment.builder()
                    .booking(booking)
                    .amount(amount)
                    .paymentMethod("PayOS")
                    .paymentDate(LocalDateTime.now())
                    .status("completed")
                    .transactionId(transactionId)
                    .build();
            Payment savedPayment = paymentRepository.save(payment);
            
            // ✅ CRITICAL: Flush immediately to ensure payment is in database
            entityManager.flush();
            System.out.println("  ✅ Payment record saved with ID: " + savedPayment.getId());
            
            // Verify payment was actually saved
            BigDecimal totalPaid = paymentRepository.sumPaidByBooking(bookingId);
            System.out.println("  📊 Total paid for this booking (after save): " + totalPaid);
        }

        // 5) Update booking status and payment state
        System.out.println("  🔄 Updating booking status and payment state...");
        
        // Re-fetch booking to get latest state
        booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        
        // Calculate total paid
        BigDecimal totalPaid = paymentRepository.sumPaidByBooking(bookingId);
        if (totalPaid == null) totalPaid = BigDecimal.ZERO;
        
        System.out.println("  📊 Total paid: " + totalPaid);
        System.out.println("  📊 Deposit amount: " + booking.getDepositAmount());
        System.out.println("  📊 Total price: " + booking.getTotalPrice());
        
        // Determine payment state
        String paymentState = "unpaid";
        if (booking.getDepositAmount() != null && totalPaid.compareTo(booking.getDepositAmount()) >= 0) {
            if (booking.getTotalPrice() != null && totalPaid.compareTo(booking.getTotalPrice()) >= 0) {
                paymentState = "paid_in_full";
                System.out.println("  ✅ Determined state: paid_in_full");
            } else {
                paymentState = "deposit_paid";
                System.out.println("  ✅ Determined state: deposit_paid");
            }
        } else {
            System.out.println("  ⚠️ Determined state: unpaid (not enough payment)");
        }
        
        // Update payment state
        booking.setPaymentState(paymentState);
        
        // Update status
        if ("deposit_paid".equals(paymentState) || "paid_in_full".equals(paymentState)) {
            booking.setStatus("confirmed");
            System.out.println("  ✅ Setting status to: confirmed");
        }
        
        // Save booking
        bookingRepository.save(booking);
        entityManager.flush();
        
        // Generate check-in code if needed
        if (("deposit_paid".equals(paymentState) || "paid_in_full".equals(paymentState))
                && (booking.getCheckInCode() == null || booking.getCheckInCode().isBlank())) {
            String code = generateCheckInCode(6);
            booking.setCheckInCode(code);
            bookingRepository.save(booking);
            entityManager.flush();
            System.out.println("  ✅ Generated check-in code: " + code);
        }
        
        // Verify final state
        booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking != null) {
            System.out.println("  ✅ Booking updated successfully!");
            System.out.println("     Final Status: " + booking.getStatus());
            System.out.println("     Final Payment State: " + booking.getPaymentState());
            System.out.println("     Check-in Code: " + (booking.getCheckInCode() != null ? booking.getCheckInCode() : "N/A"));
        }
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
}