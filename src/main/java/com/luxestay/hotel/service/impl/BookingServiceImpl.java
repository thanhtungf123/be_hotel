package com.luxestay.hotel.service.impl;

import com.luxestay.hotel.dto.PagedResponse;
import com.luxestay.hotel.dto.booking.*;
import com.luxestay.hotel.model.Account;
import com.luxestay.hotel.model.entity.BookingEntity;
import com.luxestay.hotel.model.entity.RoomEntity;
import com.luxestay.hotel.repository.*;
import com.luxestay.hotel.service.BookingService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final AccountRepository accountRepository;

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
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đặt phòng"));

        String st = (b.getStatus()==null?"pending":b.getStatus()).toLowerCase();
        if (st.equals("cancellation_requested"))
            throw new IllegalStateException("Bạn đã gửi yêu cầu hủy, vui lòng chờ duyệt");
        if (st.equals("cancelled"))
            throw new IllegalStateException("Đơn đã hủy");
        if (st.equals("checked_in") || st.equals("checked_out"))
            throw new IllegalStateException("Không thể hủy khi đã nhận/trả phòng");

        // kiểm tra hạn chót hủy miễn phí (không block, chỉ tham khảo; muốn block -> throw)
        if (b.getCheckIn() != null) {
            LocalDateTime deadline = b.getCheckIn().atStartOfDay().minusHours(CANCEL_FREE_HOURS);
            if (LocalDateTime.now().isAfter(deadline)) {
                // có thể ghi thêm note “Trễ hạn”
            }
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
        } else {
            // từ chối: quay về confirmed (hoặc pending tùy nghiệp vụ)
            b.setStatus("confirmed");
            if (note != null && !note.isBlank()) {
                b.setCancelReason(append(b.getCancelReason(), "Reject: " + note));
            }
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
    @Transactional
    @Override
    public void confirmBookingPayment(int bookingId) {

        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found: " + bookingId));

        if ("pending".equalsIgnoreCase(booking.getStatus())) {

            // 1. Cập nhật booking
            booking.setStatus("confirmed");
            bookingRepository.save(booking);

            // 2. Cập nhật phòng
            RoomEntity room = booking.getRoom();
            if (room != null) {
                room.setStatus("occupied");
                roomRepository.save(room);
            } else {
                throw new IllegalStateException("Booking " + bookingId + " has no room associated.");
            }
        } else {
            System.out.println("Booking " + bookingId + " status is already confirmed.");
        }
    }
}
