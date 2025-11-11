package com.luxestay.hotel.controller;

import com.luxestay.hotel.dto.booking.BookingRequest;
import com.luxestay.hotel.dto.booking.BookingSummary;
import com.luxestay.hotel.model.Account;
import com.luxestay.hotel.model.Payment;
import com.luxestay.hotel.model.Services;
import com.luxestay.hotel.model.entity.BookingCustomerDetails;
import com.luxestay.hotel.model.entity.BookingEntity;
import com.luxestay.hotel.model.entity.RoomEntity;
import com.luxestay.hotel.repository.BookingCustomerDetailsRepository;
import com.luxestay.hotel.repository.BookingRepository;
import com.luxestay.hotel.repository.PaymentRepository;
import com.luxestay.hotel.repository.RoomRepository;
import com.luxestay.hotel.repository.ServicesRepository;
import com.luxestay.hotel.service.AuthService;
import com.luxestay.hotel.service.BookingService;
import jakarta.persistence.EntityManager;
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

    private static final int CHECKIN_HOUR = 14;

    private final AuthService authService;
    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final BookingCustomerDetailsRepository bookingCustomerDetailsRepository;
    private final PaymentRepository paymentRepository;
    private final BookingService bookingService;
    private final ServicesRepository servicesRepository;
    private final EntityManager entityManager;

    /** Chỉ cho phép role staff|admin */
    private void ensureStaffOrAdmin(Account acc){
        String role = acc.getRole()!=null ? acc.getRole().getName() : "";
        if (!"admin".equalsIgnoreCase(role) && !"staff".equalsIgnoreCase(role)) {
            throw new IllegalArgumentException("Chỉ staff/admin");
        }
    }

    /** Walk-in booking */
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
        BigDecimal roomTotal = BigDecimal.valueOf(price).multiply(BigDecimal.valueOf(nights));

        // ✅ Calculate services total price
        BigDecimal servicesTotal = BigDecimal.ZERO;
        java.util.Set<Services> selectedServices = new java.util.HashSet<>();
        if (req.getServiceIds() != null && !req.getServiceIds().isEmpty()) {
            for (Integer serviceId : req.getServiceIds()) {
                Services service = servicesRepository.findById(serviceId)
                        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy dịch vụ với ID: " + serviceId));
                selectedServices.add(service);
                servicesTotal = servicesTotal.add(BigDecimal.valueOf(service.getPrice()));
            }
        }

        // ✅ Total = room price + services price
        BigDecimal total = roomTotal.add(servicesTotal);

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
        
        // ✅ Set services
        if (!selectedServices.isEmpty()) {
            b.setServices(selectedServices);
        }
        
        bookingRepository.save(b);
        entityManager.flush(); // Ensure booking ID is generated

        // Generate check-in code
        if (b.getId() != null) {
            b.setCheckInCode(generateCheckInCode(b.getId()));
            bookingRepository.save(b);
        }

        // Create payment record for walk-in booking (cash payment)
        Payment payment = Payment.builder()
                .booking(b)
                .amount(total)
                .paymentMethod("cash")
                .paymentDate(LocalDateTime.now())
                .status("completed")
                .transactionId("WALK-IN-" + b.getId())
                .build();
        paymentRepository.save(payment);
        entityManager.flush(); // Ensure payment is saved

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
                "paymentState", "paid_in_full",
                "totalPrice", total.intValue()
        ));
    }

    /** Lịch đặt phòng */
    @GetMapping("/room/{roomId}/schedule")
    public ResponseEntity<?> getRoomSchedule(
            @RequestHeader("X-Auth-Token") String token,
            @PathVariable Integer roomId
    ) {
        Account acc = authService.requireAccount(token);
        ensureStaffOrAdmin(acc);

        List<BookingEntity> bookings = bookingRepository.findActiveBookingsByRoom(roomId);
        List<Map<String, String>> schedule = bookings.stream()
                .map(b -> {
                    BigDecimal totalPaid = paymentRepository.sumPaidByBooking(b.getId());
                    if (totalPaid == null) totalPaid = BigDecimal.ZERO;
                    String calculatedPaymentState = calculatePaymentState(totalPaid, b.getDepositAmount(), b.getTotalPrice());
                    return Map.of(
                            "bookingId", String.valueOf(b.getId()),
                            "checkIn", b.getCheckIn().toString(),
                            "checkOut", b.getCheckOut().toString(),
                            "status", b.getStatus() != null ? b.getStatus() : "",
                            "paymentState", calculatedPaymentState
                    );
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("items", schedule));
    }

    /** Danh sách bookings */
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

        // findAll now uses EntityGraph to eager load services
        var allPage = bookingRepository.findAll(pageable);
        var all = allPage.getContent();
        var items = all.stream()
                .filter(b -> status==null || (b.getStatus()!=null && b.getStatus().equalsIgnoreCase(status)))
                .map(b -> {
                    BigDecimal totalPaid = paymentRepository.sumPaidByBooking(b.getId());
                    if (totalPaid == null) totalPaid = BigDecimal.ZERO;
                    String calculatedPaymentState = calculatePaymentState(totalPaid, b.getDepositAmount(), b.getTotalPrice());
                    if (paymentState != null && !calculatedPaymentState.equalsIgnoreCase(paymentState)) return null;
                    Map<String,Object> item = new java.util.HashMap<>();
                    item.put("id", b.getId());
                    item.put("roomName", b.getRoom()!=null? b.getRoom().getRoomName(): null);
                    item.put("checkIn", b.getCheckIn()!=null? b.getCheckIn().toString(): null);
                    item.put("checkOut", b.getCheckOut()!=null? b.getCheckOut().toString(): null);
                    item.put("totalPrice", b.getTotalPrice() != null ? b.getTotalPrice() : BigDecimal.ZERO);
                    item.put("status", b.getStatus() != null ? b.getStatus() : "");
                    item.put("paymentState", calculatedPaymentState);
                    item.put("customerName", b.getCustomerDetails()!=null? b.getCustomerDetails().getFullName(): (b.getAccount()!=null? b.getAccount().getFullName(): null));
                    item.put("checkInCode", b.getCheckInCode());
                    
                    // ✅ Add services
                    if (b.getServices() != null && !b.getServices().isEmpty()) {
                        List<Map<String, Object>> serviceList = b.getServices().stream()
                            .map(service -> {
                                Map<String, Object> svc = new java.util.HashMap<>();
                                svc.put("id", service.getId());
                                svc.put("name", service.getNameService());
                                svc.put("description", service.getDescription());
                                svc.put("price", service.getPrice());
                                return svc;
                            })
                            .toList();
                        item.put("services", serviceList);
                    }
                    
                    return item;
                })
                .filter(item -> item != null)
                .toList();

        Map<String,Object> resp = new java.util.HashMap<>();
        resp.put("items", items);
        resp.put("page", p);
        resp.put("size", s);
        resp.put("total", items.size());
        return ResponseEntity.ok(resp);
    }

    /** Chi tiết booking */
    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@RequestHeader("X-Auth-Token") String token,
                                    @PathVariable Integer id){
        Account acc = authService.requireAccount(token);
        ensureStaffOrAdmin(acc);

        BookingEntity b = bookingRepository.findByIdWithServices(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đặt phòng"));

        Map<String,Object> data = new java.util.HashMap<>();
        data.put("id", b.getId());
        data.put("roomName", b.getRoom()!=null? b.getRoom().getRoomName(): null);
        data.put("checkIn", b.getCheckIn()!=null? b.getCheckIn().toString(): null);
        data.put("checkOut", b.getCheckOut()!=null? b.getCheckOut().toString(): null);
        data.put("totalPrice", b.getTotalPrice());
        data.put("status", b.getStatus());

        BigDecimal totalPaid = paymentRepository.sumPaidByBooking(id);
        if (totalPaid == null) totalPaid = BigDecimal.ZERO;
        String calculatedPaymentState = calculatePaymentState(totalPaid, b.getDepositAmount(), b.getTotalPrice());
        data.put("paymentState", calculatedPaymentState);
        data.put("checkInCode", b.getCheckInCode());

        BookingCustomerDetails k = b.getCustomerDetails();
        Map<String,Object> cust = new java.util.HashMap<>();
        cust.put("fullName", k!=null? k.getFullName(): (b.getAccount()!=null? b.getAccount().getFullName(): null));
        cust.put("phoneNumber", k!=null? k.getPhoneNumber(): null);
        cust.put("nationalIdNumber", k!=null? k.getNationalIdNumber(): null);
        cust.put("idFrontUrl", k!=null? k.getIdFrontUrl(): null);
        cust.put("idBackUrl", k!=null? k.getIdBackUrl(): null);
        data.put("customer", cust);

        Map<String,Object> refund = new java.util.HashMap<>();
        refund.put("accountHolder", b.getRefundAccountHolder());
        refund.put("accountNumber", b.getRefundAccountNumber());
        refund.put("bankName", b.getRefundBankName());
        refund.put("submittedAt", b.getRefundSubmittedAt()!=null? b.getRefundSubmittedAt().toString(): null);
        refund.put("completedAt", b.getRefundCompletedAt()!=null? b.getRefundCompletedAt().toString(): null);
        refund.put("completedBy", b.getRefundCompletedBy());
        refund.put("hasRefundInfo", b.getRefundSubmittedAt()!=null);
        refund.put("isCompleted", b.getRefundCompletedAt()!=null);
        data.put("refund", refund);
        
        // ✅ Add services
        if (b.getServices() != null && !b.getServices().isEmpty()) {
            List<Map<String, Object>> serviceList = b.getServices().stream()
                .map(service -> {
                    Map<String, Object> svc = new java.util.HashMap<>();
                    svc.put("id", service.getId());
                    svc.put("name", service.getNameService());
                    svc.put("description", service.getDescription());
                    svc.put("price", service.getPrice());
                    return svc;
                })
                .toList();
            data.put("services", serviceList);
        }

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

        if (b.getCheckIn() == null) {
            throw new IllegalStateException("Thiếu ngày check-in");
        }
        if (LocalDate.now().isBefore(b.getCheckIn())) {
            throw new IllegalStateException("Chỉ có thể check-in khi đến ngày nhận phòng hoặc sau đó");
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

    /** Staff xác nhận hoàn tiền */
    @PostMapping("/{id}/confirm-refund")
    @Transactional
    public ResponseEntity<?> confirmRefund(@RequestHeader("X-Auth-Token") String token,
                                           @PathVariable Integer id) {
        Account acc = authService.requireAccount(token);
        ensureStaffOrAdmin(acc);

        bookingService.confirmRefundCompleted(id, acc.getId());
        return ResponseEntity.ok(Map.of(
                "bookingId", id,
                "message", "Đã xác nhận hoàn tiền thành công"
        ));
    }

    /** Tính toán trạng thái thanh toán dựa theo số tiền thực tế */
    private String calculatePaymentState(BigDecimal totalPaid, BigDecimal depositAmount, BigDecimal totalPrice) {
        if (totalPaid == null) totalPaid = BigDecimal.ZERO;
        if (totalPrice != null && totalPaid.compareTo(totalPrice) >= 0) return "paid_in_full";
        if (depositAmount != null && totalPaid.compareTo(depositAmount) >= 0) return "deposit_paid";
        return "unpaid";
    }

    /** Sinh mã check-in */
    private String generateCheckInCode(Integer bookingId) {
        return String.format("AP%06d", bookingId);
    }
}
