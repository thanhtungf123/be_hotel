package com.luxestay.hotel.controller;

import com.luxestay.hotel.model.Account;
import com.luxestay.hotel.model.Review;
import com.luxestay.hotel.model.entity.BookingEntity;
import com.luxestay.hotel.model.entity.RoomEntity;
import com.luxestay.hotel.repository.BookingRepository;
import com.luxestay.hotel.repository.RoomRepository;
import com.luxestay.hotel.response.ApiResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    // Inject các repository khác nếu cần thiết (ví dụ: AccountRepository)

    @Autowired
    public OrderController(BookingRepository bookingRepository, RoomRepository roomRepository) {
        this.bookingRepository = bookingRepository;
        this.roomRepository = roomRepository;
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<BookingEntity>> createBooking(@RequestBody Map<String, Object> requestBody) {
        try {
            // Lấy thông tin từ request body
            Integer accountId = (Integer) requestBody.get("accountId");
            Integer roomId = (Integer) requestBody.get("roomId");
            String checkInDateStr = (String) requestBody.get("checkInDate");
            String checkOutDateStr = (String) requestBody.get("checkOutDate");
            BigDecimal totalPrice = new BigDecimal(requestBody.get("totalPrice").toString());

            // Tìm kiếm Account và RoomEntity
            // Trong thực tế, bạn sẽ cần AccountRepository để tìm Account
            // Ví dụ: Account account = accountRepository.findById(accountId).orElseThrow(() -> new EntityNotFoundException("Account not found"));
            Account account = new Account();
            account.setId(accountId); // Giả định có Account đã tồn tại

            RoomEntity room = roomRepository.findById(roomId)
                    .orElseThrow(() -> new EntityNotFoundException("Room not found with ID: " + roomId));

            // Tạo đối tượng BookingEntity
            BookingEntity booking = new BookingEntity();
            booking.setAccount(account);
            booking.setRoom(room);
            booking.setCheckIn(LocalDate.parse(checkInDateStr));
            booking.setCheckOut(LocalDate.parse(checkOutDateStr));
            booking.setTotalPrice(totalPrice);
            booking.setStatus("pending"); // Trạng thái ban đầu
            booking.setCreatedAt(LocalDateTime.now());

            // Lưu booking vào database
            BookingEntity savedBooking = bookingRepository.save(booking);

            return ResponseEntity.ok(ApiResponse.success(savedBooking));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Failed to create booking: " + e.getMessage()));
        }
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<BookingEntity>> getBookingDetails(@PathVariable Integer bookingId) {
        try {
            Optional<BookingEntity> bookingOpt = bookingRepository.findById(bookingId);
            if (bookingOpt.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success(bookingOpt.get()));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Booking not found with ID: " + bookingId));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Failed to retrieve booking: " + e.getMessage()));
        }
    }

    @PutMapping("/{bookingId}/cancel")
    public ResponseEntity<ApiResponse<BookingEntity>> cancelBooking(@PathVariable Integer bookingId) {
        try {
            Optional<BookingEntity> bookingOpt = bookingRepository.findById(bookingId);
            if (!bookingOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Booking not found with ID: " + bookingId));
            }

            BookingEntity booking = bookingOpt.get();
            booking.setStatus("cancelled"); // Cập nhật trạng thái
            bookingRepository.save(booking);

            return ResponseEntity.ok(ApiResponse.success(booking));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("Failed to cancel booking: " + e.getMessage()));
        }
    }
}