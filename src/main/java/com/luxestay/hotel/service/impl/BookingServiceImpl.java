package com.luxestay.hotel.service.impl;

import com.luxestay.hotel.dto.PagedResponse;
import com.luxestay.hotel.dto.booking.*;
import com.luxestay.hotel.model.Account;
import com.luxestay.hotel.model.entity.BookingEntity;
import com.luxestay.hotel.model.entity.RoomEntity;
import com.luxestay.hotel.repository.BookingRepository;
import com.luxestay.hotel.repository.*;
import com.luxestay.hotel.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Set;


@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final AccountRepository accountRepository;

    private static final Set<String> CUSTOMER_CANCEL_ALLOWED = Set.of("pending","confirmed");

    private static final int CANCEL_FREE_HOURS = 24; // bạn tùy chỉnh

    @Override
    @Transactional
    public BookingResponse create(Integer accountId, BookingRequest req) {
        if (req.getRoomId() == null) throw new IllegalArgumentException("Thiếu roomId");
        if (req.getCheckIn() == null || req.getCheckOut() == null)
            throw new IllegalArgumentException("Thiếu ngày nhận/trả");
        LocalDate in  = LocalDate.parse(req.getCheckIn());
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
        b.setStatus("pending"); // hoặc confirmed sau thanh toán
        b.setCreatedAt(LocalDateTime.now());

        bookingRepository.save(b);
        return new BookingResponse(b.getId(), b.getStatus(), total.intValue());
    }

    @Override
    @Transactional
    public void requestCancel(Integer bookingId, Integer accountId, String reason) {
        BookingEntity b = bookingRepository.findByIdAndAccount_Id(bookingId, accountId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đặt phòng của bạn"));

        if (!CUSTOMER_CANCEL_ALLOWED.contains(b.getStatus().toLowerCase())) {
            throw new IllegalStateException("Trạng thái hiện tại không cho phép yêu cầu huỷ");
        }
        b.setStatus("cancel_requested");
        b.setCancelReason(reason);
        b.setCancelRequestedAt(LocalDateTime.now());
        bookingRepository.save(b);
    }

    @Transactional
    @Override
    public void decideCancel(Integer bookingId, Integer staffId, boolean approve, String note) {
        BookingEntity b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đặt phòng"));

        if (!"cancel_requested".equalsIgnoreCase(b.getStatus())) {
            throw new IllegalStateException("Đơn này không ở trạng thái chờ duyệt huỷ");
        }
        if (approve) {
            b.setStatus("cancelled");
        } else {
            b.setStatus("confirmed");
        }
        b.setCancelApprovedBy(staffId);
        b.setCancelApprovedAt(LocalDateTime.now());
        if (note != null && !note.isBlank()) {
            String old = b.getCancelReason() == null ? "" : b.getCancelReason() + " | ";
            b.setCancelReason(old + note);
        }
        bookingRepository.save(b);
    }

    private String append(String base, String extra) {
        if (base == null || base.isBlank()) return extra;
        return base + "\n" + extra;
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
            dto.setRoomId(b.getRoom()!=null? b.getRoom().getId(): null);
            dto.setRoomName(b.getRoom()!=null? b.getRoom().getRoomName(): null);
            dto.setCheckIn(b.getCheckIn());
            dto.setCheckOut(b.getCheckOut());
            dto.setTotalPrice(b.getTotalPrice());
            dto.setStatus(b.getStatus());
//            dto.setCreatedAt(b.getCreatedAt());
            return dto;
        }).toList();

        return new PagedResponse<>(items, (int)pg.getTotalElements(), pg.getNumber(), pg.getSize());
    }
}
