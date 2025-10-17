package com.luxestay.hotel.service;

import com.luxestay.hotel.dto.booking.BookingRequest;
import com.luxestay.hotel.dto.booking.BookingResponse;

public interface BookingService {
    BookingResponse create(Integer accountId, BookingRequest req);
}
