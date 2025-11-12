package com.luxestay.hotel.service;

import com.luxestay.hotel.dto.invoice.InvoiceDTO;

public interface InvoiceService {
    /**
     * Generate invoice for a booking
     * @param bookingId Booking ID
     * @return InvoiceDTO
     */
    InvoiceDTO generateInvoice(Integer bookingId);
    
    /**
     * Send invoice email to customer
     * @param bookingId Booking ID
     */
    void sendInvoiceEmail(Integer bookingId);
}

