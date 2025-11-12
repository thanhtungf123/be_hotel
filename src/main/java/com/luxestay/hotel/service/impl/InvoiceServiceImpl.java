package com.luxestay.hotel.service.impl;

import com.luxestay.hotel.dto.invoice.InvoiceDTO;
import com.luxestay.hotel.model.entity.BookingEntity;
import com.luxestay.hotel.repository.BookingRepository;
import com.luxestay.hotel.repository.PaymentRepository;
import com.luxestay.hotel.service.EmailService;
import com.luxestay.hotel.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {
    
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final EmailService emailService;
    
    @Override
    public InvoiceDTO generateInvoice(Integer bookingId) {
        BookingEntity booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy booking"));
        
        InvoiceDTO invoice = new InvoiceDTO();
        
        // Invoice info
        invoice.setBookingId(bookingId);
        invoice.setInvoiceNumber(generateInvoiceNumber(bookingId));
        invoice.setIssueDate(LocalDateTime.now());
        
        // Customer info
        if (booking.getCustomerDetails() != null) {
            invoice.setCustomerName(booking.getCustomerDetails().getFullName());
            invoice.setCustomerPhone(booking.getCustomerDetails().getPhoneNumber());
        } else if (booking.getAccount() != null) {
            invoice.setCustomerName(booking.getAccount().getFullName());
        }
        
        if (booking.getAccount() != null) {
            invoice.setCustomerEmail(booking.getAccount().getEmail());
        }
        
        // Booking info
        if (booking.getRoom() != null) {
            invoice.setRoomName(booking.getRoom().getRoomName());
        }
        
        invoice.setCheckIn(booking.getCheckIn());
        invoice.setCheckOut(booking.getCheckOut());
        invoice.setCheckInCode(booking.getCheckInCode());
        invoice.setAdults(booking.getAdults());
        invoice.setChildren(booking.getChildren());
        
        // Calculate nights
        if (booking.getCheckIn() != null && booking.getCheckOut() != null) {
            long nights = ChronoUnit.DAYS.between(booking.getCheckIn(), booking.getCheckOut());
            invoice.setNights((int) Math.max(1, nights));
        }
        
        // Pricing breakdown
        invoice.setSubtotal(booking.getSubtotalPrice() != null ? booking.getSubtotalPrice() : BigDecimal.ZERO);
        invoice.setTax(booking.getTaxAmount() != null ? booking.getTaxAmount() : BigDecimal.ZERO);
        invoice.setServiceFee(booking.getServiceFeeAmount() != null ? booking.getServiceFeeAmount() : BigDecimal.ZERO);
        invoice.setTotal(booking.getTotalPrice() != null ? booking.getTotalPrice() : BigDecimal.ZERO);
        invoice.setDepositAmount(booking.getDepositAmount() != null ? booking.getDepositAmount() : BigDecimal.ZERO);
        
        // Calculate room and services breakdown
        BigDecimal servicesTotal = BigDecimal.ZERO;
        List<InvoiceDTO.ServiceItem> serviceItems = new ArrayList<>();
        
        if (booking.getServices() != null && !booking.getServices().isEmpty()) {
            for (var service : booking.getServices()) {
                BigDecimal price = BigDecimal.valueOf(service.getPrice());
                servicesTotal = servicesTotal.add(price);
                serviceItems.add(new InvoiceDTO.ServiceItem(
                    service.getId(),
                    service.getNameService(),
                    service.getDescription(),
                    price
                ));
            }
        }
        
        invoice.setServicesTotal(servicesTotal);
        invoice.setServices(serviceItems);
        
        BigDecimal roomTotal = invoice.getSubtotal().subtract(servicesTotal);
        invoice.setRoomTotal(roomTotal);
        
        // Payment info
        invoice.setPaymentState(booking.getPaymentState());
        BigDecimal paidAmount = paymentRepository.sumPaidByBooking(bookingId);
        invoice.setPaidAmount(paidAmount != null ? paidAmount : BigDecimal.ZERO);
        
        return invoice;
    }
    
    @Override
    public void sendInvoiceEmail(Integer bookingId) {
        InvoiceDTO invoice = generateInvoice(bookingId);
        
        if (invoice.getCustomerEmail() == null || invoice.getCustomerEmail().isBlank()) {
            throw new IllegalStateException("Không có email khách hàng");
        }
        
        emailService.sendInvoiceEmail(invoice.getCustomerEmail(), invoice);
    }
    
    private String generateInvoiceNumber(Integer bookingId) {
        long timestamp = System.currentTimeMillis() / 1000;
        return String.format("INV-%06d-%d", bookingId, timestamp);
    }
}

