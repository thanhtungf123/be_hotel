package com.luxestay.hotel.service.impl;

import com.luxestay.hotel.dto.PagedResponse;
import com.luxestay.hotel.dto.booking.BookingSummary;
import com.luxestay.hotel.model.entity.BookingEntity;
import com.luxestay.hotel.repository.BookingRepository;
import com.luxestay.hotel.repository.PaymentRepository;
import com.luxestay.hotel.service.BookingQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingQueryServiceImpl implements BookingQueryService {

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<BookingSummary> listMine(Integer accountId, String status, Pageable pageable) {
        Page<BookingEntity> page = bookingRepository.findForHistory(accountId, status, pageable);

        List<BookingSummary> items = page.getContent().stream().map(b -> {
            BookingSummary s = new BookingSummary();
            s.setId(b.getId());
            if (b.getRoom() != null) {
                s.setRoomId(b.getRoom().getId());
                s.setRoomName(b.getRoom().getRoomName());
                s.setRoomImageUrl(b.getRoom().getImageUrl());
                s.setBedLayout(b.getRoom().getBedLayout() != null ? b.getRoom().getBedLayout().getLayoutName() : null);
                // Set adults và children từ booking
                s.setAdults(b.getAdults() != null ? b.getAdults() : 1);
                s.setChildren(b.getChildren() != null ? b.getChildren() : 0);
                // Tính tổng guests để backward compatible
                int totalGuests = s.getAdults() + s.getChildren();
                s.setGuests(totalGuests > 0 ? totalGuests : (b.getRoom().getCapacity() != null ? b.getRoom().getCapacity() : 0));
                s.setCancelReason(b.getCancelReason());
            }
            s.setCheckIn(b.getCheckIn());
            s.setCheckOut(b.getCheckOut());
            long nights = 0;
            if (b.getCheckIn() != null && b.getCheckOut() != null) {
                nights = Math.max(ChronoUnit.DAYS.between(b.getCheckIn(), b.getCheckOut()), 0);
            }
            s.setNights(nights);
            s.setTotalPrice(b.getTotalPrice());
            s.setStatus(b.getStatus());

            // NEW: payment summary
            s.setDepositAmount(b.getDepositAmount());
            var paid = paymentRepository.sumPaidByBooking(b.getId());
            if (paid == null) paid = java.math.BigDecimal.ZERO;
            s.setAmountPaid(paid);
            if (b.getTotalPrice()!=null) s.setAmountRemaining(b.getTotalPrice().subtract(paid));
            
            // ✅ CRITICAL: Calculate payment state based on ACTUAL payments, not stored value
            // This ensures payment state is always accurate even if syncPaymentStatus wasn't called
            String calculatedPaymentState = calculatePaymentState(paid, b.getDepositAmount(), b.getTotalPrice());
            s.setPaymentState(calculatedPaymentState);
            
            s.setCheckInCode(b.getCheckInCode());
            
            // Refund information
            s.setRefundAccountHolder(b.getRefundAccountHolder());
            s.setRefundAccountNumber(b.getRefundAccountNumber());
            s.setRefundBankName(b.getRefundBankName());
            s.setRefundSubmitted(b.getRefundSubmittedAt() != null);
            s.setRefundCompleted(b.getRefundCompletedAt() != null);
            
            // ✅ Services information
            if (b.getServices() != null && !b.getServices().isEmpty()) {
                List<BookingSummary.ServiceInfo> serviceInfos = b.getServices().stream()
                    .map(service -> new BookingSummary.ServiceInfo(
                        service.getId(),
                        service.getNameService(),
                        service.getDescription(),
                        service.getPrice()
                    ))
                    .toList();
                s.setServices(serviceInfos);
            }
            
            return s;
        }).toList();

        return new PagedResponse<>(items, page.getTotalElements(), page.getNumber(), page.getSize());
    }

    // (tuỳ chọn) helper build summary cho GET /bookings/{id}
    public BookingSummary summaryOf(BookingEntity b){
        var s = new BookingSummary();
        s.setId(b.getId());
        if (b.getRoom() != null) {
            s.setRoomId(b.getRoom().getId());
            s.setRoomName(b.getRoom().getRoomName());
            s.setRoomImageUrl(b.getRoom().getImageUrl());
            s.setBedLayout(b.getRoom().getBedLayout() != null ? b.getRoom().getBedLayout().getLayoutName() : null);
            // Set adults và children từ booking
            s.setAdults(b.getAdults() != null ? b.getAdults() : 1);
            s.setChildren(b.getChildren() != null ? b.getChildren() : 0);
            // Tính tổng guests để backward compatible
            int totalGuests = s.getAdults() + s.getChildren();
            s.setGuests(totalGuests > 0 ? totalGuests : (b.getRoom().getCapacity() != null ? b.getRoom().getCapacity() : 0));
        }
        s.setCheckIn(b.getCheckIn());
        s.setCheckOut(b.getCheckOut());
        s.setTotalPrice(b.getTotalPrice());
        s.setStatus(b.getStatus());
        s.setDepositAmount(b.getDepositAmount());
        var paid = paymentRepository.sumPaidByBooking(b.getId());
        if (paid == null) paid = java.math.BigDecimal.ZERO;
        s.setAmountPaid(paid);
        if (b.getTotalPrice()!=null) s.setAmountRemaining(b.getTotalPrice().subtract(paid));
        
        // ✅ CRITICAL: Calculate payment state based on ACTUAL payments, not stored value
        // This ensures payment state is always accurate even if syncPaymentStatus wasn't called
        String calculatedPaymentState = calculatePaymentState(paid, b.getDepositAmount(), b.getTotalPrice());
        s.setPaymentState(calculatedPaymentState);
        
        s.setCheckInCode(b.getCheckInCode());
        
        // Refund information
        s.setRefundAccountHolder(b.getRefundAccountHolder());
        s.setRefundAccountNumber(b.getRefundAccountNumber());
        s.setRefundBankName(b.getRefundBankName());
        s.setRefundSubmitted(b.getRefundSubmittedAt() != null);
        s.setRefundCompleted(b.getRefundCompletedAt() != null);
        
        // ✅ Services information
        if (b.getServices() != null && !b.getServices().isEmpty()) {
            List<BookingSummary.ServiceInfo> serviceInfos = b.getServices().stream()
                .map(service -> new BookingSummary.ServiceInfo(
                    service.getId(),
                    service.getNameService(),
                    service.getDescription(),
                    service.getPrice()
                ))
                .toList();
            s.setServices(serviceInfos);
        }
        
        return s;
    }
    
    /**
     * Calculate payment state based on actual paid amount
     * This ensures consistency even if booking.paymentState is out of sync
     */
    private String calculatePaymentState(java.math.BigDecimal totalPaid, java.math.BigDecimal depositAmount, java.math.BigDecimal totalPrice) {
        if (totalPaid == null) totalPaid = java.math.BigDecimal.ZERO;
        
        // If paid >= total price → paid_in_full
        if (totalPrice != null && totalPaid.compareTo(totalPrice) >= 0) {
            return "paid_in_full";
        }
        
        // If paid >= deposit amount → deposit_paid
        if (depositAmount != null && totalPaid.compareTo(depositAmount) >= 0) {
            return "deposit_paid";
        }
        
        // Otherwise → unpaid
        return "unpaid";
    }
}
