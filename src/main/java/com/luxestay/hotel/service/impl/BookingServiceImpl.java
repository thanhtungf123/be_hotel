package com.luxestay.hotel.service.impl;

import com.luxestay.hotel.dto.PagedResponse;
import com.luxestay.hotel.dto.booking.*;
import com.luxestay.hotel.model.Account;
import com.luxestay.hotel.model.Services;
import com.luxestay.hotel.model.entity.BookingCustomerDetails;
import com.luxestay.hotel.model.entity.BookingEntity;
import com.luxestay.hotel.model.entity.RoomEntity;
import com.luxestay.hotel.repository.*;
import com.luxestay.hotel.service.BookingService;
import com.luxestay.hotel.service.EmailService;
import com.luxestay.hotel.service.NotificationService;
import com.luxestay.hotel.util.RefundPolicyCalculator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final int DEFAULT_DEPOSIT_PERCENT = 30;
    private static final int CANCEL_FREE_HOURS = 24;

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final AccountRepository accountRepository;
    private final PaymentRepository paymentRepository;
    private final BookingCustomerDetailsRepository bookingCustomerDetailsRepository;
    private final ServicesRepository servicesRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public BookingResponse create(Integer accountId, BookingRequest req) {
        if (req.getRoomId() == null) throw new IllegalArgumentException("Thiếu roomId");
        if (req.getCheckIn() == null || req.getCheckOut() == null)
            throw new IllegalArgumentException("Thiếu ngày nhận/trả");

        LocalDate in = LocalDate.parse(req.getCheckIn());
        LocalDate out = LocalDate.parse(req.getCheckOut());
        if (!out.isAfter(in)) throw new IllegalArgumentException("Ngày trả phải sau ngày nhận");

        int adults = req.getAdults() != null ? req.getAdults() : 1;
        int children = req.getChildren() != null ? req.getChildren() : 0;
        if (adults < 1) throw new IllegalArgumentException("Số người lớn phải ≥ 1");
        if (children < 0) throw new IllegalArgumentException("Số trẻ em phải ≥ 0");

        RoomEntity room = roomRepository.findById(req.getRoomId().intValue())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng"));
        Account acc = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản"));

        int equivalentAdults = adults + (int) Math.ceil(children / 2.0);
        if (room.getCapacity() != null && equivalentAdults > room.getCapacity()) {
            throw new IllegalArgumentException("Số khách quy đổi (" + equivalentAdults +
                    " người lớn, trong đó: " + adults + " người lớn + " + children +
                    " trẻ em) vượt quá sức chứa phòng (" + room.getCapacity() + ")");
        }

        boolean conflict = bookingRepository.hasActiveConflict(room.getId(), in, out);
        if (conflict) {
            throw new IllegalStateException("Phòng đã được giữ bởi booking khác (đã cọc/đã xác nhận)");
        }

        long nights = Math.max(1, ChronoUnit.DAYS.between(in, out));
        int price = room.getPricePerNight() == null ? 0 : room.getPricePerNight();
        BigDecimal roomTotal = BigDecimal.valueOf(price).multiply(BigDecimal.valueOf(nights));

        // ✅ Calculate services total price
        BigDecimal servicesTotal = BigDecimal.ZERO;
        Set<Services> selectedServices = new HashSet<>();
        if (req.getServiceIds() != null && !req.getServiceIds().isEmpty()) {
            for (Integer serviceId : req.getServiceIds()) {
                Services service = servicesRepository.findById(serviceId)
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dịch vụ với ID: " + serviceId));
                selectedServices.add(service);
                servicesTotal = servicesTotal.add(BigDecimal.valueOf(service.getPrice()));
            }
        }

        // ✅ Calculate subtotal (room + services)
        BigDecimal subtotal = roomTotal.add(servicesTotal);

        // ✅ Calculate tax (10%) and service fee (5%)
        BigDecimal tax = subtotal.multiply(BigDecimal.valueOf(0.10))
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal serviceFee = subtotal.multiply(BigDecimal.valueOf(0.05))
                .setScale(0, RoundingMode.HALF_UP);

        // ✅ Total = subtotal + tax + service fee
        BigDecimal total = subtotal.add(tax).add(serviceFee);

        int percent = (req.getDepositPercent() != null && req.getDepositPercent() > 0 && req.getDepositPercent() < 100)
                ? req.getDepositPercent() : DEFAULT_DEPOSIT_PERCENT;
        BigDecimal deposit = total.multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);

        BookingEntity b = new BookingEntity();
        b.setAccount(acc);
        b.setRoom(room);
        b.setCheckIn(in);
        b.setCheckOut(out);
        b.setAdults(adults);
        b.setChildren(children);
        b.setSubtotalPrice(subtotal);
        b.setTaxAmount(tax);
        b.setServiceFeeAmount(serviceFee);
        b.setTotalPrice(total);
        b.setDepositAmount(deposit);
        b.setPaymentState("unpaid");
        b.setStatus("pending");
        b.setCreatedAt(LocalDateTime.now());
        
        // ✅ Set services
        if (!selectedServices.isEmpty()) {
            b.setServices(selectedServices);
        }
        
        bookingRepository.save(b);

        if (b.getId() != null) {
            b.setCheckInCode(generateCheckInCode(b.getId()));
            bookingRepository.save(b);
        }

        BookingCustomerDetails k = new BookingCustomerDetails();
        k.setBooking(b);
        k.setFullName(req.getFullName());
        k.setGender(req.getGender());
        k.setPhoneNumber(req.getPhoneNumber());
        k.setNationalIdNumber(req.getNationalIdNumber());
        if (req.getDateOfBirth() != null && !req.getDateOfBirth().isBlank()) {
            try {
                k.setDateOfBirth(LocalDate.parse(req.getDateOfBirth()));
            } catch (Exception ignore) {}
        }
        k.setIdFrontUrl(req.getIdFrontUrl());
        k.setIdBackUrl(req.getIdBackUrl());
        k.setBankAccountName(req.getBankAccountName());
        k.setBankAccountNumber(req.getBankAccountNumber());
        k.setBankName(req.getBankName());
        k.setBankCode(req.getBankCode());
        k.setBankBranch(req.getBankBranch());
        k.setCreatedAt(LocalDateTime.now());
        bookingCustomerDetailsRepository.save(k);

        // Prepare services list for response
        List<Map<String, Object>> servicesList = new ArrayList<>();
        for (Services service : selectedServices) {
            Map<String, Object> svc = new java.util.HashMap<>();
            svc.put("id", service.getId());
            svc.put("name", service.getNameService());
            svc.put("description", service.getDescription());
            svc.put("price", service.getPrice());
            servicesList.add(svc);
        }

        // ✅ Send booking success notification
        try {
            notificationService.notifyBookingSuccess(
                acc.getId(),
                b.getId(),
                room.getRoomName(),
                req.getCheckIn(),
                req.getCheckOut()
            );
        } catch (Exception e) {
            System.err.println("Failed to send booking notification: " + e.getMessage());
        }

        return new BookingResponse(
            b.getId(), 
            b.getStatus(), 
            total.intValue(), 
            subtotal.intValue(),
            tax.intValue(),
            serviceFee.intValue(),
            deposit.intValue(), 
            b.getPaymentState(),
            servicesList
        );
    }

    @Override
    @Transactional
    public void onPaymentCaptured(int bookingId) {
        System.out.println("  [onPaymentCaptured] Starting for booking ID: " + bookingId);

        var b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found: " + bookingId));

        System.out.println("  [onPaymentCaptured] Fetching total paid amount...");
        BigDecimal paid = paymentRepository.sumPaidByBooking(bookingId);
        if (paid == null) paid = BigDecimal.ZERO;

        System.out.println("  [onPaymentCaptured] Total paid: " + paid);
        System.out.println("  [onPaymentCaptured] Deposit amount: " + b.getDepositAmount());
        System.out.println("  [onPaymentCaptured] Total price: " + b.getTotalPrice());

        String state = "unpaid";
        if (b.getDepositAmount() != null && paid.compareTo(b.getDepositAmount()) >= 0) {
            if (b.getTotalPrice() != null && paid.compareTo(b.getTotalPrice()) >= 0) {
                state = "paid_in_full";
                System.out.println("  [onPaymentCaptured] Determined state: paid_in_full");
            } else {
                state = "deposit_paid";
                System.out.println("  [onPaymentCaptured] Determined state: deposit_paid");
            }
        } else {
            System.out.println("  [onPaymentCaptured] Determined state: unpaid (not enough payment)");
        }

        b.setPaymentState(state);
        if ("deposit_paid".equals(state) || "paid_in_full".equals(state)) {
            // ✅ Set to pending_verification - Staff needs to verify room first
            b.setStatus("pending_verification");
            if (b.getCheckInCode() == null || b.getCheckInCode().isBlank()) {
                b.setCheckInCode(generateCheckInCode(b.getId()));
            }
        }

        System.out.println("  [onPaymentCaptured] Saving booking...");
        bookingRepository.save(b);
        System.out.println("  [onPaymentCaptured] Booking saved successfully!");

        if (("deposit_paid".equals(state) || "paid_in_full".equals(state))
                && (b.getCheckInCode() == null || b.getCheckInCode().isBlank())) {
            b.setCheckInCode(generateCheckInCode(b.getId()));
            bookingRepository.save(b);
            try {
                String email = b.getAccount() != null ? b.getAccount().getEmail() : null;
                String customerName = b.getCustomerDetails() != null
                        ? b.getCustomerDetails().getFullName()
                        : (b.getAccount() != null ? b.getAccount().getFullName() : "Quý khách");
                String roomName = b.getRoom() != null ? b.getRoom().getRoomName() : "";
                String in = b.getCheckIn() != null ? b.getCheckIn().toString() : "";
                String out = b.getCheckOut() != null ? b.getCheckOut().toString() : "";
                if (email != null && !email.isBlank()) {
                    emailService.sendBookingConfirmation(email, customerName, roomName, in, out, state, b.getCheckInCode());
                }
                
                // ✅ Send booking confirmed notification with check-in code
                if (b.getAccount() != null && b.getCheckInCode() != null) {
                    notificationService.notifyBookingConfirmed(
                        b.getAccount().getId(),
                        b.getId(),
                        b.getCheckInCode()
                    );
                }
            } catch (Exception e) {
                System.err.println("Failed to send booking confirmation email: " + e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public void requestCancel(Integer bookingId, Integer accountId, String reason) {
        BookingEntity b = bookingRepository.findByIdAndAccount_Id(bookingId, accountId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đặt phòng"));

        String st = (b.getStatus() == null ? "pending" : b.getStatus()).toLowerCase();
        if (st.equals("cancellation_requested") || st.equals("cancel_requested"))
            throw new IllegalStateException("Bạn đã gửi yêu cầu hủy, vui lòng chờ duyệt");
        if (st.equals("cancelled"))
            throw new IllegalStateException("Đơn đã hủy");
        if (st.equals("checked_in") || st.equals("checked_out"))
            throw new IllegalStateException("Không thể hủy khi đã nhận/trả phòng");

        // ✅ Tính toán refund policy
        if (b.getCheckIn() != null) {
            BigDecimal totalPaid = paymentRepository.sumPaidByBooking(bookingId);
            if (totalPaid == null) totalPaid = BigDecimal.ZERO;
            
            // Xác định loại thanh toán: full payment hay deposit
            boolean isFullPayment = "paid_in_full".equals(b.getPaymentState());
            
            // Tính toán refund
            RefundPolicyCalculator.RefundResult refundResult = 
                RefundPolicyCalculator.calculateRefund(b.getCheckIn(), totalPaid, isFullPayment);
            
            if (!refundResult.isCanCancel()) {
                throw new IllegalStateException(refundResult.getMessage());
            }
            
            // Lưu thông tin refund
            b.setRefundAmount(refundResult.getRefundAmount());
            b.setRefundPercent(refundResult.getRefundPercent());
        }

        b.setStatus("cancel_requested");
        b.setCancelReason(reason);
        b.setCancelRequestedAt(LocalDateTime.now());
        bookingRepository.save(b);
    }

    @Override
    @Transactional
    public void decideCancel(Integer bookingId, Integer staffId, boolean approve, String note) {
        BookingEntity b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đặt phòng"));

        if (!"cancel_requested".equalsIgnoreCase(b.getStatus())) {
            throw new IllegalStateException("Đơn không ở trạng thái chờ hủy");
        }

        if (approve) {
            b.setStatus("cancelled");
            b.setCancelApprovedBy(staffId);
            b.setCancelApprovedAt(LocalDateTime.now());
            if (note != null && !note.isBlank()) {
                b.setCancelReason(append(b.getCancelReason(), "Staff note: " + note));
            }
            bookingRepository.save(b);

            try {
                String customerEmail = b.getAccount() != null ? b.getAccount().getEmail() : null;
                String customerName = b.getCustomerDetails() != null
                        ? b.getCustomerDetails().getFullName()
                        : (b.getAccount() != null ? b.getAccount().getFullName() : "Quý khách");
                String roomName = b.getRoom() != null ? b.getRoom().getRoomName() : "";
                String totalPrice = b.getTotalPrice() != null
                        ? b.getTotalPrice().toPlainString() + " VNĐ"
                        : "0 VNĐ";

                if (customerEmail != null && !customerEmail.isBlank()) {
                    emailService.sendRefundInfoRequestEmail(customerEmail, customerName, bookingId, roomName, totalPrice);
                    System.out.println("✅ Sent refund info request email to: " + customerEmail);
                }
                
                // ✅ Send cancellation approved notification
                if (b.getAccount() != null) {
                    notificationService.notifyCancellationApproved(
                        b.getAccount().getId(),
                        b.getId(),
                        roomName
                    );
                }
            } catch (Exception e) {
                System.err.println("⚠️ Failed to send refund info request email: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            b.setStatus("confirmed");
            if (note != null && !note.isBlank()) {
                b.setCancelReason(append(b.getCancelReason(), "Reject: " + note));
            }
            bookingRepository.save(b);
            
            // ✅ Send cancellation rejected notification
            try {
                if (b.getAccount() != null && b.getRoom() != null) {
                    String roomName = b.getRoom().getRoomName();
                    String reason = note != null && !note.isBlank() ? note : "Không đủ điều kiện hủy";
                    notificationService.notifyCancellationRejected(
                        b.getAccount().getId(),
                        b.getId(),
                        roomName,
                        reason
                    );
                }
            } catch (Exception e) {
                System.err.println("Failed to send cancellation rejected notification: " + e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public void submitRefundInfo(Integer bookingId, Integer accountId, RefundInfoRequest request) {
        BookingEntity b = bookingRepository.findByIdAndAccount_Id(bookingId, accountId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đặt phòng"));

        if (!"cancelled".equalsIgnoreCase(b.getStatus())) {
            throw new IllegalStateException("Chỉ có thể cung cấp thông tin hoàn tiền cho đơn đã hủy");
        }

        if (b.getRefundSubmittedAt() != null) {
            throw new IllegalStateException("Thông tin hoàn tiền đã được gửi trước đó");
        }

        if (request.getAccountHolder() == null || request.getAccountHolder().isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập tên chủ tài khoản");
        }
        if (request.getAccountNumber() == null || request.getAccountNumber().isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập số tài khoản");
        }
        if (request.getBankName() == null || request.getBankName().isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập tên ngân hàng");
        }

        b.setRefundAccountHolder(request.getAccountHolder().trim());
        b.setRefundAccountNumber(request.getAccountNumber().trim());
        b.setRefundBankName(request.getBankName().trim());
        b.setRefundSubmittedAt(LocalDateTime.now());
        bookingRepository.save(b);
        
        // ✅ Send refund processing notification
        try {
            if (b.getAccount() != null && b.getRefundAmount() != null) {
                notificationService.notifyRefundProcessing(
                    b.getAccount().getId(),
                    b.getId(),
                    b.getRefundAmount().longValue()
                );
            }
        } catch (Exception e) {
            System.err.println("Failed to send refund processing notification: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void confirmRefundCompleted(Integer bookingId, Integer staffId) {
        BookingEntity b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đặt phòng"));

        if (!"cancelled".equalsIgnoreCase(b.getStatus())) {
            throw new IllegalStateException("Chỉ có thể xác nhận hoàn tiền cho đơn đã hủy");
        }

        if (b.getRefundSubmittedAt() == null) {
            throw new IllegalStateException("Khách hàng chưa cung cấp thông tin hoàn tiền");
        }

        if (b.getRefundCompletedAt() != null) {
            throw new IllegalStateException("Đã xác nhận hoàn tiền trước đó");
        }

        b.setRefundCompletedAt(LocalDateTime.now());
        b.setRefundCompletedBy(staffId);
        bookingRepository.save(b);

        try {
            String customerEmail = b.getAccount() != null ? b.getAccount().getEmail() : null;
            String customerName = b.getCustomerDetails() != null
                    ? b.getCustomerDetails().getFullName()
                    : (b.getAccount() != null ? b.getAccount().getFullName() : "Quý khách");
            String roomName = b.getRoom() != null ? b.getRoom().getRoomName() : "";
            String refundAmount = b.getTotalPrice() != null
                    ? b.getTotalPrice().toPlainString() + " VNĐ"
                    : "0 VNĐ";

            if (customerEmail != null && !customerEmail.isBlank()) {
                emailService.sendRefundCompletedEmail(customerEmail, customerName, bookingId, roomName, refundAmount);
                System.out.println("✅ Sent refund completed email to: " + customerEmail);
            }
            
            // ✅ Send refund completed notification
            if (b.getAccount() != null && b.getRefundAmount() != null) {
                notificationService.notifyRefundCompleted(
                    b.getAccount().getId(),
                    b.getId(),
                    b.getRefundAmount().longValue()
                );
            }
        } catch (Exception e) {
            System.err.println("⚠️ Failed to send refund completed email: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String append(String base, String extra) {
        if (base == null || base.isBlank()) return extra;
        return base + "\n" + extra;
    }

    private String generateCheckInCode(Integer bookingId) {
        return String.format("AP%06d", bookingId);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<BookingSummary> history(Integer accountId, String status, Integer page, Integer size) {
        int p = page == null ? 0 : Math.max(0, page);
        int s = size == null ? 10 : Math.max(1, size);
        Pageable pageable = PageRequest.of(p, s);

        var pg = bookingRepository.findForHistory(accountId, status, pageable);

        List<BookingSummary> items = pg.getContent().stream().map(b -> {
            BookingSummary dto = new BookingSummary();
            dto.setId(b.getId());
            dto.setRoomId(b.getRoom() != null ? b.getRoom().getId() : null);
            dto.setRoomName(b.getRoom() != null ? b.getRoom().getRoomName() : null);
            dto.setCheckIn(b.getCheckIn());
            dto.setCheckOut(b.getCheckOut());
            dto.setTotalPrice(b.getTotalPrice());
            dto.setStatus(b.getStatus());
            return dto;
        }).toList();

        return new PagedResponse<>(items, (int) pg.getTotalElements(), pg.getNumber(), pg.getSize());
    }

    @Override
    @Transactional
    public void confirmBookingPayment(int bookingId) {
        var booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found: " + bookingId));
        if (!"confirmed".equalsIgnoreCase(booking.getStatus())) {
            booking.setStatus("confirmed");
            bookingRepository.save(booking);
        }
    }
}
