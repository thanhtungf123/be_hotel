package com.luxestay.hotel.service.impl;

import com.luxestay.hotel.dto.PagedResponse;
import com.luxestay.hotel.dto.booking.*;
import com.luxestay.hotel.model.Account;
import com.luxestay.hotel.model.entity.BookingCustomerDetails;
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
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;

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

        // ✅ Chỉ block khi có cọc / confirmed:
        // -> Tại thời điểm tạo, nếu đã tồn tại booking khác (cùng phòng, overlap)
        //    với status ∈ {confirmed, checked_in} hoặc paymentState ∈ {deposit_paid, paid_in_full}
        //    thì từ chối tạo.
        boolean conflict = bookingRepository.hasActiveConflict(room.getId(), in, out);
        if (conflict) {
            throw new IllegalStateException("Phòng đã được giữ bởi booking khác (đã cọc/đã xác nhận)");
        }

        long nights = Math.max(1, ChronoUnit.DAYS.between(in, out));
        int price = room.getPricePerNight() == null ? 0 : room.getPricePerNight();
        BigDecimal total = BigDecimal.valueOf(price).multiply(BigDecimal.valueOf(nights));

        int percent = (req.getDepositPercent()!=null && req.getDepositPercent()>0 && req.getDepositPercent()<100)
                ? req.getDepositPercent() : DEFAULT_DEPOSIT_PERCENT;
        BigDecimal deposit = total.multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);

        BookingEntity b = new BookingEntity();
        b.setAccount(acc);
        b.setRoom(room);
        b.setCheckIn(in);
        b.setCheckOut(out);
        b.setTotalPrice(total);
        b.setDepositAmount(deposit);
        b.setPaymentState("unpaid");
        b.setStatus("pending");
        b.setCreatedAt(LocalDateTime.now());
        bookingRepository.save(b);

        // KYC snapshot
        BookingCustomerDetails k = new BookingCustomerDetails();
        k.setBooking(b);
        k.setFullName(req.getFullName());
        k.setGender(req.getGender());
        k.setPhoneNumber(req.getPhoneNumber());
        k.setNationalIdNumber(req.getNationalIdNumber());
        if (req.getDateOfBirth()!=null && !req.getDateOfBirth().isBlank()) {
            try { k.setDateOfBirth(LocalDate.parse(req.getDateOfBirth())); } catch (Exception ignore) {}
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

        return new BookingResponse(b.getId(), b.getStatus(), total.intValue(), deposit.intValue(), b.getPaymentState());
    }

    @Override
    @Transactional
    public void onPaymentCaptured(int bookingId) {
        var b = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found: " + bookingId));

        BigDecimal paid = paymentRepository.sumPaidByBooking(bookingId);
        if (paid == null) paid = BigDecimal.ZERO;

        String state = "unpaid";
        if (b.getTotalPrice()!=null && paid.compareTo(b.getTotalPrice()) >= 0) {
            state = "paid_in_full";
        } else if (b.getDepositAmount()!=null && paid.compareTo(b.getDepositAmount()) >= 0) {
            state = "deposit_paid";
        }
        b.setPaymentState(state);

        // Sau khi có cọc/full -> chuyển confirmed
        if (!"confirmed".equalsIgnoreCase(b.getStatus())
                && ("deposit_paid".equals(state) || "paid_in_full".equals(state))) {
            b.setStatus("confirmed");
        }
        bookingRepository.save(b);
    }

    @Override
    @Transactional
    public void requestCancel(Integer bookingId, Integer accountId, String reason) {
        BookingEntity b = bookingRepository.findByIdAndAccount_Id(bookingId, accountId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đặt phòng"));

        String st = (b.getStatus()==null ? "pending" : b.getStatus()).toLowerCase();
        if (st.equals("cancellation_requested") || st.equals("cancel_requested"))
            throw new IllegalStateException("Bạn đã gửi yêu cầu hủy, vui lòng chờ duyệt");
        if (st.equals("cancelled"))
            throw new IllegalStateException("Đơn đã hủy");
        if (st.equals("checked_in") || st.equals("checked_out"))
            throw new IllegalStateException("Không thể hủy khi đã nhận/trả phòng");

        // ✅ Chặn hủy khi đã quá hạn 24h trước 00:00 ngày check-in
        if (b.getCheckIn() != null) {
            LocalDateTime deadline = b.getCheckIn().atStartOfDay().minusHours(CANCEL_FREE_HOURS);
            if (LocalDateTime.now().isAfter(deadline)) {
                throw new IllegalStateException("Đã quá hạn hủy miễn phí 24h trước check-in");
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
            // từ chối: quay về confirmed
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
            return dto;
        }).toList();

        return new PagedResponse<>(items, (int)pg.getTotalElements(), pg.getNumber(), pg.getSize());
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
        var room = booking.getRoom();
        if (room != null) {
            // giữ nguyên behaviour: phòng sẽ không xuất hiện trong search vì status != 'available'
            room.setStatus("reserved");
            roomRepository.save(room);
        }
    }
}
