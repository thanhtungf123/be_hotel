package com.luxestay.hotel.service.impl;

import com.luxestay.hotel.dto.booking.BookingRequest;
import com.luxestay.hotel.dto.booking.BookingResponse;
import com.luxestay.hotel.model.Account;
import com.luxestay.hotel.model.entity.BookingEntity;
import com.luxestay.hotel.model.entity.RoomEntity;
import com.luxestay.hotel.repository.AccountRepository;
import com.luxestay.hotel.repository.BookingRepository;
import com.luxestay.hotel.repository.RoomRepository;
import com.luxestay.hotel.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public BookingResponse create(Integer accountId, BookingRequest req) {
        if (req.getRoomId() == null) throw new IllegalArgumentException("Thiếu roomId");
        if (req.getCheckIn() == null || req.getCheckOut() == null)
            throw new IllegalArgumentException("Thiếu ngày nhận/trả");
        LocalDate in = LocalDate.parse(req.getCheckIn());
        LocalDate out = LocalDate.parse(req.getCheckOut());
        if (!out.isAfter(in)) throw new IllegalArgumentException("Ngày trả phải sau ngày nhận");

        RoomEntity room = roomRepository.findById(req.getRoomId().intValue())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng"));
        Account acc = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản"));

        long nights = Math.max(1, ChronoUnit.DAYS.between(in, out));
        int price = room.getPricePerNight() == null ? 0 : room.getPricePerNight();
        BigDecimal total = BigDecimal.valueOf(price).multiply(BigDecimal.valueOf(nights));

        BookingEntity b = new BookingEntity();
        b.setAccount(acc);
        b.setRoom(room);
        b.setCheckIn(in);
        b.setCheckOut(out);
        b.setTotalPrice(total);
        b.setStatus("pending");           // có thể đổi thành confirmed sau khi thanh toán
        b.setCreatedAt(LocalDateTime.now());

        bookingRepository.save(b);
        return new BookingResponse(b.getId(), b.getStatus(), total.intValue());
    }
}
