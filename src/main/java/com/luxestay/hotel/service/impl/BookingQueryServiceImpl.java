// service/impl/BookingQueryServiceImpl.java
package com.luxestay.hotel.service.impl;

import com.luxestay.hotel.dto.PagedResponse;
import com.luxestay.hotel.dto.booking.BookingSummary;
import com.luxestay.hotel.model.entity.BookingEntity;
import com.luxestay.hotel.repository.BookingRepository;
import com.luxestay.hotel.service.BookingQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingQueryServiceImpl implements BookingQueryService {

    private final BookingRepository bookingRepository;

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
                s.setGuests(b.getRoom().getCapacity());
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
            return s;
        }).toList();

        return new PagedResponse<>(items, page.getTotalElements(), page.getNumber(), page.getSize());
    }
}
