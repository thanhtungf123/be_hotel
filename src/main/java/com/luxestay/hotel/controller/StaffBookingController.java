package com.luxestay.hotel.controller;

import com.luxestay.hotel.dto.booking.BookingRequest;
import com.luxestay.hotel.model.Account;
import com.luxestay.hotel.model.entity.BookingCustomerDetails;
import com.luxestay.hotel.model.entity.BookingEntity;
import com.luxestay.hotel.model.entity.RoomEntity;
import com.luxestay.hotel.repository.BookingCustomerDetailsRepository;
import com.luxestay.hotel.repository.BookingRepository;
import com.luxestay.hotel.repository.RoomRepository;
import com.luxestay.hotel.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/staff/bookings")
@RequiredArgsConstructor
public class StaffBookingController {

    private static final int CHECKIN_HOUR = 14; // giờ check-in mặc định

    private final AuthService authService;
    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final BookingCustomerDetailsRepository bookingCustomerDetailsRepository;

    /** Chỉ cho phép role staff|admin */
    private void ensureStaffOrAdmin(Account acc){
        String role = acc.getRole()!=null ? acc.getRole().getName() : "";
        if (!"admin".equalsIgnoreCase(role) && !"staff".equalsIgnoreCase(role)) {
            throw new IllegalArgumentException("Chỉ staff/admin");
        }
    }

    /** Walk-in booking: staff tạo booking trực tiếp tại quầy */
    @PostMapping("/walk-in")
    @Transactional
    public ResponseEntity<?> createWalkInBooking(
            @RequestHeader("X-Auth-Token") String token,
            @RequestBody BookingRequest req
    ) {
        Account staff = authService.requireAccount(token);
        ensureStaffOrAdmin(staff);

        if (req.getRoomId() == null) throw new IllegalArgumentException("Thiếu roomId");
        if (req.getCheckIn() == null || req.getCheckOut() == null)
            throw new IllegalArgumentException("Thiếu ngày nhận/trả");

        LocalDate in = LocalDate.parse(req.getCheckIn());
        LocalDate out = LocalDate.parse(req.getCheckOut());
        if (!out.isAfter(in)) throw new IllegalArgumentException("Ngày trả phải sau ngày nhận");

        RoomEntity room = roomRepository.findById(req.getRoomId().intValue())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng"));

        boolean conflict = bookingRepository.hasActiveConflict(room.getId(), in, out);
        if (conflict) throw new IllegalStateException("Phòng đã được giữ bởi booking khác");

        long nights = Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(in, out));
        int price = room.getPricePerNight() == null ? 0 : room.getPricePerNight();
        BigDecimal total = BigDecimal.valueOf(price).multiply(BigDecimal.valueOf(nights));

        BookingEntity b = new BookingEntity();
        b.setAccount(staff);
        b.setRoom(room);
        b.setCheckIn(in);
        b.setCheckOut(out);
        b.setTotalPrice(total);
        b.setDepositAmount(BigDecimal.ZERO);
        b.setPaymentState("paid_in_full");
        b.setStatus("confirmed");
        b.setCreatedAt(LocalDateTime.now());
        bookingRepository.save(b);
        
        if (b.getId() != null) {
            b.setCheckInCode(generateCheckInCode(b.getId()));
            bookingRepository.save(b);
        }

        BookingCustomerDetails k = new BookingCustomerDetails();
        k.setBooking(b);
        k.setFullName(req.getFullName());
        k.setGender(req.getGender());
        k.setPhoneNumber(req.getPhoneNumber());
        k.setNationalIdNumber(req.getNationalIdNumber());
        if (req.getDateOfBirth() != null && !req.getDateOfBirth().isBlank()) {
            try { k.setDateOfBirth(LocalDate.parse(req.getDateOfBirth())); } catch (Exception ignore) {}
        }
        k.setCreatedAt(LocalDateTime.now());
        bookingCustomerDetailsRepository.save(k);

        return ResponseEntity.ok(Map.of(
                "bookingId", b.getId(),
                "status", "confirmed",
                "totalPrice", total.intValue()
        ));
    }

    /** Lịch đặt phòng (để staff xem phòng nào đã bị giữ) */
    @GetMapping("/room/{roomId}/schedule")
    public ResponseEntity<?> getRoomSchedule(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Integer roomId
    ) {
        Account acc = authService.requireAccount(token);
        ensureStaffOrAdmin(acc);

        List<BookingEntity> bookings = bookingRepository.findActiveBookingsByRoom(roomId);
        List<Map<String, String>> schedule = bookings.stream()
                .map(b -> Map.of(
                        "bookingId", String.valueOf(b.getId()),
                        "checkIn", b.getCheckIn().toString(),
                        "checkOut", b.getCheckOut().toString(),
                        "status", b.getStatus() != null ? b.getStatus() : "",
                        "paymentState", b.getPaymentState() != null ? b.getPaymentState() : ""
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("items", schedule));
    }

    /** Danh sách bookings cho staff/admin */
    @GetMapping
    public ResponseEntity<?> list(
            @RequestHeader("X-Auth-Token") String token,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "paymentState", required = false) String paymentState,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size
    ){
        Account acc = authService.requireAccount(token);
        ensureStaffOrAdmin(acc);

        int p = page==null?0:Math.max(0,page);
        int s = size==null?20:Math.max(1, Math.min(100,size));
        Pageable pageable = PageRequest.of(p, s);

        var all = bookingRepository.findAll(pageable).getContent();
        var items = all.stream()
                .filter(b -> status==null || (b.getStatus()!=null && b.getStatus().equalsIgnoreCase(status)))
                .filter(b -> paymentState==null || (b.getPaymentState()!=null && b.getPaymentState().equalsIgnoreCase(paymentState)))
                .map(b -> Map.of(
                        "id", b.getId(),
                        "roomName", b.getRoom()!=null? b.getRoom().getRoomName(): null,
                        "checkIn", b.getCheckIn()!=null? b.getCheckIn().toString(): null,
                        "checkOut", b.getCheckOut()!=null? b.getCheckOut().toString(): null,
                        "totalPrice", b.getTotalPrice(),
                        "status", b.getStatus(),
                        "paymentState", b.getPaymentState(),
                        "customerName", b.getCustomerDetails()!=null? b.getCustomerDetails().getFullName(): (b.getAccount()!=null? b.getAccount().getFullName(): null),
                        "checkInCode", b.getCheckInCode() != null ? b.getCheckInCode() : (b.getId() != null ? generateCheckInCode(b.getId()) : null)
                ))
                .toList();

        java.util.Map<String,Object> resp = new java.util.HashMap<>();
        resp.put("items", items);
        resp.put("page", p);
        resp.put("size", s);
        resp.put("total", items.size());
        return ResponseEntity.ok(resp);
    }

    /** Chi tiết một booking cho staff */
    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@RequestHeader("X-Auth-Token") String token,
                                    @PathVariable Integer id){
        Account acc = authService.requireAccount(token);
        ensureStaffOrAdmin(acc);

        BookingEntity b = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đặt phòng"));

        BookingCustomerDetails k = b.getCustomerDetails();
        java.util.Map<String,Object> data = new java.util.HashMap<>();
        data.put("id", b.getId());
        data.put("roomName", b.getRoom()!=null? b.getRoom().getRoomName(): null);
        data.put("checkIn", b.getCheckIn()!=null? b.getCheckIn().toString(): null);
        data.put("checkOut", b.getCheckOut()!=null? b.getCheckOut().toString(): null);
        data.put("totalPrice", b.getTotalPrice());
        data.put("status", b.getStatus());
        data.put("paymentState", b.getPaymentState());
        data.put("checkInCode", b.getCheckInCode() != null ? b.getCheckInCode() : (b.getId() != null ? generateCheckInCode(b.getId()) : null));

        java.util.Map<String,Object> cust = new java.util.HashMap<>();
        cust.put("fullName", k!=null? k.getFullName(): (b.getAccount()!=null? b.getAccount().getFullName(): null));
        cust.put("phoneNumber", k!=null? k.getPhoneNumber(): null);
        cust.put("nationalIdNumber", k!=null? k.getNationalIdNumber(): null);
        cust.put("idFrontUrl", k!=null? k.getIdFrontUrl(): null);
        cust.put("idBackUrl", k!=null? k.getIdBackUrl(): null);
        data.put("customer", cust);
        return ResponseEntity.ok(data);
    }

    /** Check-in */
    @PostMapping("/{id}/check-in")
    public ResponseEntity<?> checkIn(@RequestHeader("X-Auth-Token") String token,
                                     @PathVariable Integer id){
        Account acc = authService.requireAccount(token);
        ensureStaffOrAdmin(acc);

        BookingEntity b = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đặt phòng"));

        if (!"confirmed".equalsIgnoreCase(b.getStatus())) {
            throw new IllegalStateException("Chỉ check-in khi booking ở trạng thái confirmed");
        }

        b.setStatus("checked_in");
        bookingRepository.save(b);

        RoomEntity r = b.getRoom();
        if (r != null) {
            r.setStatus("occupied");
            roomRepository.save(r);
        }
        return ResponseEntity.ok(Map.of("bookingId", id, "status", "checked_in"));
    }

    /** Check-out */
    @PostMapping("/{id}/check-out")
    public ResponseEntity<?> checkOut(@RequestHeader("X-Auth-Token") String token,
                                      @PathVariable Integer id){
        Account acc = authService.requireAccount(token);
        ensureStaffOrAdmin(acc);

        BookingEntity b = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đặt phòng"));

        b.setStatus("checked_out");
        bookingRepository.save(b);

        RoomEntity r = b.getRoom();
        if (r != null) {
            r.setStatus("available");
            roomRepository.save(r);
        }
        return ResponseEntity.ok(Map.of("bookingId", id, "status", "checked_out"));
    }

    /** No-show */
    @PostMapping("/{id}/mark-no-show")
    public ResponseEntity<?> markNoShow(@RequestHeader("X-Auth-Token") String token,
                                        @PathVariable Integer id){
        Account acc = authService.requireAccount(token);
        ensureStaffOrAdmin(acc);

        BookingEntity b = bookingRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đặt phòng"));

        if (!"confirmed".equalsIgnoreCase(b.getStatus())) {
            throw new IllegalStateException("Chỉ áp dụng cho đơn đã xác nhận");
        }
        if (b.getCheckIn() == null) {
            throw new IllegalStateException("Thiếu ngày check-in");
        }

        LocalDateTime threshold = b.getCheckIn().atTime(CHECKIN_HOUR, 0).plusHours(5);
        if (LocalDateTime.now().isBefore(threshold)) {
            throw new IllegalStateException("Chỉ được đánh no-show sau giờ check-in + 5h");
        }

        b.setStatus("cancelled");
        b.setCancelReason((b.getCancelReason()==null?"":"\n")
                + "Đánh dấu no-show bởi staff vào " + LocalDateTime.now());
        bookingRepository.save(b);

        RoomEntity r = b.getRoom();
        if (r != null) {
            r.setStatus("available");
            roomRepository.save(r);
        }
        return ResponseEntity.ok(Map.of("bookingId", id, "status", "cancelled"));
    }

    private String generateCheckInCode(Integer bookingId) {
        return String.format("AP%06d", bookingId);
    }
}
