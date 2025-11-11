package com.luxestay.hotel.controller;

import com.luxestay.hotel.model.entity.BookingEntity;
import com.luxestay.hotel.model.Payment;
import com.luxestay.hotel.repository.BookingRepository;
import com.luxestay.hotel.repository.PaymentRepository;
import com.luxestay.hotel.response.ApiResponse;
import com.luxestay.hotel.service.EmailService;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
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
    private final EntityManager entityManager;
    private final EmailService emailService;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Autowired
    public CheckoutController(PayOS payOS, BookingRepository bookingRepository,
            PaymentRepository paymentRepository, EntityManager entityManager,
            EmailService emailService) {
        this.payOS = payOS;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.entityManager = entityManager;
        this.emailService = emailService;
    }

    @PostMapping("/{bookingId}/create-payment-link")
    public ResponseEntity<ApiResponse<CreatePaymentLinkResponse>> createPaymentLink(
            @PathVariable("bookingId") Integer bookingId,
            @RequestParam(name = "purpose", defaultValue = "full") String purpose) {
        try {
            Optional<BookingEntity> bookingOpt = bookingRepository.findById(bookingId);
            if (bookingOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Booking not found with ID: " + bookingId));
            }
            BookingEntity booking = bookingOpt.get();

            // Số tiền theo mục đích
            BigDecimal paid = paymentRepository.sumPaidByBooking(bookingId);
            if (paid == null)
                paid = BigDecimal.ZERO;

            BigDecimal amountDecimal;
            switch (purpose.toLowerCase()) {
                case "deposit" -> amountDecimal = booking.getDepositAmount();
                case "balance" -> amountDecimal = booking.getTotalPrice().subtract(paid).max(BigDecimal.ZERO);
                default -> amountDecimal = booking.getTotalPrice();
            }

            if (amountDecimal == null || amountDecimal.compareTo(BigDecimal.ZERO) <= 0)
                return ResponseEntity.badRequest().body(ApiResponse.error("Invalid amount."));

            // ✅ FIX: Tạo orderCode duy nhất cho mỗi purpose để tránh lỗi "Đơn thanh toán đã tồn tại"
            // Format: bookingId * 100 + purpose_code
            // purpose_code: 1 = deposit, 2 = balance, 0 = full
            int purposeCode;
            switch (purpose.toLowerCase()) {
                case "deposit" -> purposeCode = 1;
                case "balance" -> purposeCode = 2;
                default -> purposeCode = 0; // full
            }
            long orderCode = (long) booking.getId() * 100L + purposeCode;
            
            System.out.println("╔═══════════════════════════════════════════════════════════════╗");
            System.out.println("║        CREATE PAYMENT LINK                                    ║");
            System.out.println("╠═══════════════════════════════════════════════════════════════╣");
            System.out.println("  Booking ID: " + booking.getId());
            System.out.println("  Purpose: " + purpose + " (code: " + purposeCode + ")");
            System.out.println("  Order Code: " + orderCode);
            System.out.println("  Amount: " + amountDecimal);
            System.out.println("╚═══════════════════════════════════════════════════════════════╝");
            
            long amount = amountDecimal.longValue();
            String description = "Booking #" + booking.getId() + " " + purpose;

            String beBase = "http://localhost:8080";
            String returnUrl = beBase + "/api/checkout/return?bookingId=" + booking.getId() + "&purpose=" + purpose;
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
            System.out.println("  ✅ Payment link created successfully");
            return ResponseEntity.ok(ApiResponse.success(payosResponse));
        } catch (Exception e) {
            System.err.println("╔═══════════════════════════════════════════════════════════════╗");
            System.err.println("║        ❌ CREATE PAYMENT LINK ERROR ❌                        ║");
            System.err.println("╠═══════════════════════════════════════════════════════════════╣");
            System.err.println("  Booking ID: " + bookingId);
            System.err.println("  Purpose: " + purpose);
            System.err.println("  Error: " + e.getMessage());
            System.err.println("╚═══════════════════════════════════════════════════════════════╝");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to create payment link: " + e.getMessage()));
        }
    }

    @GetMapping("/return")
    public ResponseEntity<Void> handleReturn(
            @RequestParam Integer bookingId,
            @RequestParam(required = false) String purpose,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String id,
            @RequestParam(required = false) Long orderCode) {
        
        System.out.println("╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║        RETURN HANDLER - PROCESSING PAYMENT RETURN              ║");
        System.out.println("╠═══════════════════════════════════════════════════════════════╣");
        System.out.println("  Booking ID: " + bookingId);
        System.out.println("  Purpose: " + purpose);
        System.out.println("  Code: " + code);
        System.out.println("  Status: " + status);
        System.out.println("  Transaction ID: " + id);
        System.out.println("  Order Code: " + orderCode);
        
        // ✅ CRITICAL FIX: Nếu thanh toán thành công (code=00), tạo payment record và update status ngay lập tức
        // Không cần đợi webhook!
        if ("00".equals(code) && bookingId != null) {
            try {
                BookingEntity booking = bookingRepository.findById(bookingId).orElse(null);
                if (booking == null) {
                    System.err.println("  ❌ Booking not found: " + bookingId);
                } else {
                    System.out.println("  ✅ Found booking in database");
                    System.out.println("     Current Status: " + booking.getStatus());
                    System.out.println("     Current Payment State: " + booking.getPaymentState());
                    System.out.println("     Total Price: " + booking.getTotalPrice());
                    System.out.println("     Deposit Amount: " + booking.getDepositAmount());
                    
                    // Check if payment already exists for this transaction
                    BigDecimal totalPaid = paymentRepository.sumPaidByBooking(bookingId);
                    if (totalPaid == null) totalPaid = BigDecimal.ZERO;
                    
                    System.out.println("     Total Paid (before): " + totalPaid);
                    
                    // ✅ FIX: Check if payment for this specific transaction already exists
                    String transactionIdToUse = id != null ? id : "RETURN_" + System.currentTimeMillis();
                    boolean paymentExistsForTransaction = paymentRepository.findAll().stream()
                            .anyMatch(p -> p.getBooking() != null 
                                    && p.getBooking().getId().equals(bookingId)
                                    && transactionIdToUse.equals(p.getTransactionId())
                                    && "completed".equalsIgnoreCase(p.getStatus()));
                    
                    // ✅ CRITICAL: Tạo payment record nếu chưa có cho transaction này
                    // Đặc biệt quan trọng cho balance payment (đã có deposit rồi)
                    if (!paymentExistsForTransaction) {
                        System.out.println("  ⚠️ No payment record found for this transaction. Creating payment record from return URL...");
                        
                        // Determine payment amount based on purpose
                        BigDecimal paymentAmount = null;
                        
                        if (purpose != null) {
                            switch (purpose.toLowerCase()) {
                                case "deposit":
                                    paymentAmount = booking.getDepositAmount();
                                    System.out.println("  💡 Purpose: deposit → Using deposit amount: " + paymentAmount);
                                    break;
                                case "balance":
                                    // ✅ FIX: Balance = Total - Đã trả (có thể đã có deposit)
                                    paymentAmount = booking.getTotalPrice().subtract(totalPaid).max(BigDecimal.ZERO);
                                    System.out.println("  💡 Purpose: balance → Using balance amount: " + paymentAmount);
                                    System.out.println("     Total Price: " + booking.getTotalPrice());
                                    System.out.println("     Already Paid: " + totalPaid);
                                    break;
                                case "full":
                                default:
                                    paymentAmount = booking.getTotalPrice();
                                    System.out.println("  💡 Purpose: full → Using total price: " + paymentAmount);
                                    break;
                            }
                        }
                        
                        // Fallback: nếu không có purpose, dùng deposit nếu có, nếu không thì total
                        if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
                            if (booking.getDepositAmount() != null && booking.getDepositAmount().compareTo(BigDecimal.ZERO) > 0) {
                                paymentAmount = booking.getDepositAmount();
                                System.out.println("  💡 Fallback: Using deposit amount: " + paymentAmount);
                            } else {
                                paymentAmount = booking.getTotalPrice();
                                System.out.println("  💡 Fallback: Using total price: " + paymentAmount);
                            }
                        }
                        
                        if (paymentAmount != null && paymentAmount.compareTo(BigDecimal.ZERO) > 0) {
                            // Create payment record
                            Payment payment = Payment.builder()
                                    .booking(booking)
                                    .amount(paymentAmount)
                                    .paymentMethod("PayOS")
                                    .paymentDate(LocalDateTime.now())
                                    .status("completed")
                                    .transactionId(transactionIdToUse)
                                    .build();
                            
                            Payment savedPayment = paymentRepository.save(payment);
                            entityManager.flush();
                            
                            System.out.println("  ✅ Payment record created with ID: " + savedPayment.getId());
                            System.out.println("     Amount: " + paymentAmount);
                            
                            // ✅ CRITICAL: Recalculate total paid AFTER flush to ensure new payment is included
                            totalPaid = paymentRepository.sumPaidByBooking(bookingId);
                            if (totalPaid == null) totalPaid = BigDecimal.ZERO;
                            System.out.println("     Total Paid (after creating payment): " + totalPaid);
                            System.out.println("     Total Price: " + booking.getTotalPrice());
                            System.out.println("     Comparison: totalPaid >= totalPrice? " + (totalPaid.compareTo(booking.getTotalPrice()) >= 0));
                        } else {
                            System.err.println("  ❌ Could not determine payment amount!");
                        }
                    } else {
                        System.out.println("  ℹ️ Payment record already exists for transaction: " + transactionIdToUse);
                    }
                    
                    // ✅ CRITICAL: Re-fetch totalPaid after all payment operations to ensure accuracy
                    totalPaid = paymentRepository.sumPaidByBooking(bookingId);
                    if (totalPaid == null) totalPaid = BigDecimal.ZERO;
                    System.out.println("  📊 Final Total Paid (before sync): " + totalPaid);
                    
                    // Update booking status và payment state
                    if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
                        // Re-fetch booking để đảm bảo có latest state
                        booking = bookingRepository.findById(bookingId).orElse(null);
                        if (booking != null) {
                            System.out.println("  📊 Current Payment State: " + booking.getPaymentState());
                            System.out.println("  📊 Expected Payment State based on totalPaid: " + 
                                (totalPaid.compareTo(booking.getTotalPrice()) >= 0 ? "paid_in_full" : 
                                 totalPaid.compareTo(booking.getDepositAmount()) >= 0 ? "deposit_paid" : "unpaid"));
                            
                            // ✅ CRITICAL: Always sync payment status to ensure check-in code is generated
                            syncPaymentStatus(bookingId);
                            
                            // Verify final state
                            booking = bookingRepository.findById(bookingId).orElse(null);
                            if (booking != null) {
                                System.out.println("  ✅ Final Status: " + booking.getStatus());
                                System.out.println("  ✅ Final Payment State: " + booking.getPaymentState());
                                System.out.println("  ✅ Final Check-in Code: " + (booking.getCheckInCode() != null ? booking.getCheckInCode() : "N/A"));
                            }
                        }
                    } else {
                        System.out.println("  ℹ️ No payment found. Payment may not have been processed yet.");
                    }
                }
                
                System.out.println("╚═══════════════════════════════════════════════════════════════╝");
            } catch (Exception e) {
                System.err.println("╔═══════════════════════════════════════════════════════════════╗");
                System.err.println("║                    ❌ RETURN HANDLER ERROR ❌                  ║");
                System.err.println("╠═══════════════════════════════════════════════════════════════╣");
                System.err.println("  Booking ID: " + bookingId);
                System.err.println("  Error: " + e.getMessage());
                System.err.println("  Stack trace:");
                e.printStackTrace();
                System.err.println("╚═══════════════════════════════════════════════════════════════╝");
                // Continue to redirect even if update fails
            }
        } else {
            System.out.println("  ℹ️ Payment not successful (code: " + code + ") or bookingId missing");
            System.out.println("╚═══════════════════════════════════════════════════════════════╝");
        }
        
        // Redirect to frontend success page with purpose parameter
        String feBase = frontendBaseUrl != null ? frontendBaseUrl : "http://localhost:5173";
        String location = feBase.replaceAll("/+$", "") + "/payment/success?bookingId=" + bookingId;
        // ✅ FIX: Thêm purpose vào URL để PaymentSuccess có thể phân biệt
        if (purpose != null && !purpose.trim().isEmpty()) {
            location += "&purpose=" + purpose;
        }
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
            var map = mapper.convertValue(data, new TypeReference<java.util.Map<String, Object>>() {
            });
            String code = String.valueOf(map.getOrDefault("code", ""));
            String idStr = String.valueOf(map.getOrDefault("id", ""));
            long orderCode = Long.parseLong(String.valueOf(map.getOrDefault("orderCode", "0")));
            
            // ✅ FIX: Extract bookingId từ orderCode
            // orderCode format: bookingId * 100 + purpose_code
            // => bookingId = orderCode / 100
            int bookingId = (int) (orderCode / 100);
            int purposeCode = (int) (orderCode % 100);
            
            System.out.println("  📦 Order Code from webhook: " + orderCode);
            System.out.println("  📦 Extracted Booking ID: " + bookingId);
            System.out.println("  📦 Extracted Purpose Code: " + purposeCode);
            
            // Lấy số tiền thực tế từ webhook nếu có
            java.math.BigDecimal paidAmount = null;
            try {
                Object amtObj = map.get("amount");
                Object dataNode = map.get("data");
                if (amtObj == null && dataNode instanceof java.util.Map<?,?> dm) {
                    Object innerAmt = ((java.util.Map<?,?>) dm).get("amount");
                    Object innerData = ((java.util.Map<?,?>) dm).get("data");
                    if (innerAmt == null && innerData instanceof java.util.Map<?,?> deeper) {
                        innerAmt = deeper.get("amount");
                    }
                    amtObj = innerAmt;
                }
                if (amtObj != null) paidAmount = new java.math.BigDecimal(String.valueOf(amtObj));
            } catch (Exception ignore) {}

            if ("00".equals(code)) {
                System.out.println("╔═══════════════════════════════════════════════════════════════╗");
                System.out.println("║    CHECKOUT WEBHOOK RECEIVED - CODE 00 (SUCCESS)             ║");
                System.out.println("╠═══════════════════════════════════════════════════════════════╣");
                System.out.println("  Order Code: " + orderCode);
                System.out.println("  Booking ID: " + bookingId);
                System.out.println("  Purpose Code: " + purposeCode);
                System.out.println("  Transaction ID: " + idStr);
                System.out.println("  Paid Amount from webhook: " + paidAmount);
                
                try {
                    processPaymentWebhook(bookingId, idStr, paidAmount);
                    System.out.println("╚═══════════════════════════════════════════════════════════════╝");
                    
                } catch (Exception e) {
                    System.err.println("╔═══════════════════════════════════════════════════════════════╗");
                    System.err.println("║                    ❌ CHECKOUT WEBHOOK ERROR ❌               ║");
                    System.err.println("╠═══════════════════════════════════════════════════════════════╣");
                    System.err.println("  Order Code: " + orderCode);
                    System.err.println("  Booking ID: " + bookingId);
                    System.err.println("  Error: " + e.getMessage());
                    System.err.println("  Stack trace:");
                    e.printStackTrace();
                    System.err.println("╚═══════════════════════════════════════════════════════════════╝");
                }
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
        
        // ✅ CRITICAL: Flush any pending payment changes before calculating total
        entityManager.flush();
        
        // Calculate total paid
        BigDecimal totalPaid = paymentRepository.sumPaidByBooking(bookingId);
        if (totalPaid == null) totalPaid = BigDecimal.ZERO;
        
        System.out.println("  📊 Total paid: " + totalPaid);
        System.out.println("  📊 Deposit amount: " + booking.getDepositAmount());
        System.out.println("  📊 Total price: " + booking.getTotalPrice());
        
        // Determine payment state
        String paymentState = "unpaid";
        if (booking.getDepositAmount() != null && totalPaid.compareTo(booking.getDepositAmount()) >= 0) {
            // ✅ FIX: So sánh chính xác với totalPrice, cho phép >= (lớn hơn hoặc bằng)
            if (booking.getTotalPrice() != null && totalPaid.compareTo(booking.getTotalPrice()) >= 0) {
                paymentState = "paid_in_full";
                System.out.println("  ✅ Determined state: paid_in_full (totalPaid: " + totalPaid + " >= totalPrice: " + booking.getTotalPrice() + ")");
            } else {
                paymentState = "deposit_paid";
                System.out.println("  ⚠️ Determined state: deposit_paid (totalPaid: " + totalPaid + " < totalPrice: " + booking.getTotalPrice() + ")");
            }
        } else {
            System.out.println("  ⚠️ Determined state: unpaid (totalPaid: " + totalPaid + " < depositAmount: " + booking.getDepositAmount() + ")");
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
                    System.out.println("  ✅ Sent booking confirmation email to: " + email);
                }
            } catch (Exception e) {
                System.err.println("  ⚠️ Failed to send booking confirmation email: " + e.getMessage());
                e.printStackTrace();
            }
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
    
    @Transactional
    private void syncPaymentStatus(int bookingId) {
        System.out.println("  [syncPaymentStatus] Starting for booking ID: " + bookingId);
        
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        
        // ✅ CRITICAL: Flush any pending changes first
        entityManager.flush();
        
        BigDecimal totalPaid = paymentRepository.sumPaidByBooking(bookingId);
        if (totalPaid == null) totalPaid = BigDecimal.ZERO;
        
        System.out.println("  [syncPaymentStatus] Total Paid: " + totalPaid);
        System.out.println("  [syncPaymentStatus] Deposit Amount: " + booking.getDepositAmount());
        System.out.println("  [syncPaymentStatus] Total Price: " + booking.getTotalPrice());
        
        String paymentState = "unpaid";
        if (booking.getDepositAmount() != null && totalPaid.compareTo(booking.getDepositAmount()) >= 0) {
            // ✅ FIX: So sánh chính xác với totalPrice, cho phép >= (lớn hơn hoặc bằng)
            if (booking.getTotalPrice() != null && totalPaid.compareTo(booking.getTotalPrice()) >= 0) {
                paymentState = "paid_in_full";
                System.out.println("  [syncPaymentStatus] ✅ Determined: paid_in_full (totalPaid: " + totalPaid + " >= totalPrice: " + booking.getTotalPrice() + ")");
            } else {
                paymentState = "deposit_paid";
                System.out.println("  [syncPaymentStatus] Determined: deposit_paid (totalPaid: " + totalPaid + " < totalPrice: " + booking.getTotalPrice() + ")");
            }
        } else {
            System.out.println("  [syncPaymentStatus] Determined: unpaid (totalPaid: " + totalPaid + " < depositAmount: " + booking.getDepositAmount() + ")");
        }
        
        booking.setPaymentState(paymentState);
        if ("deposit_paid".equals(paymentState) || "paid_in_full".equals(paymentState)) {
            booking.setStatus("confirmed");
        }
        
        bookingRepository.save(booking);
        entityManager.flush();
        
        System.out.println("  [syncPaymentStatus] ✅ Updated booking - Status: " + booking.getStatus() + ", PaymentState: " + booking.getPaymentState());
        
        // Generate check-in code if needed
        if (("deposit_paid".equals(paymentState) || "paid_in_full".equals(paymentState))
                && (booking.getCheckInCode() == null || booking.getCheckInCode().isBlank())) {
            String code = generateCheckInCode(6);
            booking.setCheckInCode(code);
            bookingRepository.save(booking);
            entityManager.flush();
            System.out.println("  ✅ Generated check-in code: " + code);
            
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
                    System.out.println("  ✅ Sent booking confirmation email to: " + email);
                }
            } catch (Exception e) {
                System.err.println("  ⚠️ Failed to send booking confirmation email: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}