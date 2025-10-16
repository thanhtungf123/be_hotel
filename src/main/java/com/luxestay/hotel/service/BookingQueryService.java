// service/BookingQueryService.java
package com.luxestay.hotel.service;

import com.luxestay.hotel.dto.PagedResponse;
import com.luxestay.hotel.dto.booking.BookingSummary;
import org.springframework.data.domain.Pageable;

public interface BookingQueryService {
    PagedResponse<BookingSummary> listMine(Integer accountId, String status, Pageable pageable);
}
