package com.luxestay.hotel.controller;

import com.luxestay.hotel.dto.review.CreateReviewRequest;
import com.luxestay.hotel.dto.review.ReviewDTO;
import com.luxestay.hotel.dto.review.ReviewableBookingDTO;
import com.luxestay.hotel.dto.review.RoomRatingDTO;
import com.luxestay.hotel.model.Account;
import com.luxestay.hotel.service.AuthService;
import com.luxestay.hotel.service.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://127.0.0.1:5173",
        "http://localhost:4173",
        "http://localhost:3000"
})
public class ReviewController {
    private final ReviewService reviewService;
    private final AuthService authService;

    @PostMapping
    public ResponseEntity<ReviewDTO> createReview(
            @RequestBody CreateReviewRequest request,
            HttpServletRequest httpRequest) {
        Account account = authService.requireAccount(
                httpRequest.getHeader("X-Auth-Token"));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.createReview(request, account));
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<ReviewDTO>> getReviewsByRoom(
            @PathVariable("roomId") Integer roomId) {
        return ResponseEntity.ok(reviewService.getReviewsByRoom(roomId));
    }

    @GetMapping("/room/{roomId}/rating")
    public ResponseEntity<RoomRatingDTO> getRoomRating(
            @PathVariable("roomId") Integer roomId) {
        return ResponseEntity.ok(reviewService.getRoomRating(roomId));
    }

    @GetMapping("/featured")
    public ResponseEntity<List<ReviewDTO>> getFeaturedReviews(
            @RequestParam(value = "limit", required = false, defaultValue = "6") Integer limit) {
        return ResponseEntity.ok(reviewService.getFeaturedReviews(limit));
    }

    /**
     * Get reviewable bookings for a user and room
     * GET /api/reviews/room/{roomId}/reviewable-bookings
     * Requires: X-Auth-Token header
     */
    @GetMapping("/room/{roomId}/reviewable-bookings")
    public ResponseEntity<List<ReviewableBookingDTO>> getReviewableBookings(
            @PathVariable("roomId") Integer roomId,
            HttpServletRequest httpRequest) {
        try {
            String token = httpRequest.getHeader("X-Auth-Token");
            if (token == null || token.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(List.of());
            }
            Account account = authService.requireAccount(token);
            return ResponseEntity.ok(reviewService.getReviewableBookings(account.getId(), roomId));
        } catch (IllegalArgumentException e) {
            // Token không hợp lệ hoặc chưa đăng nhập
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(List.of());
        } catch (Exception e) {
            // Lỗi khác (có thể do bảng reviews chưa tồn tại)
            System.err.println("Error in getReviewableBookings: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.OK)
                    .body(List.of());
        }
    }
}
