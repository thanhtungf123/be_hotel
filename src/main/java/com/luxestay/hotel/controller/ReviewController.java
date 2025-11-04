package com.luxestay.hotel.controller;

import com.luxestay.hotel.dto.review.CreateReviewRequest;
import com.luxestay.hotel.dto.review.ReviewDTO;
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
}

