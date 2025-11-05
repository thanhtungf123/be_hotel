package com.luxestay.hotel.service;

import com.luxestay.hotel.dto.review.CreateReviewRequest;
import com.luxestay.hotel.dto.review.ReviewDTO;
import com.luxestay.hotel.dto.review.RoomRatingDTO;
import com.luxestay.hotel.model.Account;
import com.luxestay.hotel.model.entity.BookingEntity;
import com.luxestay.hotel.model.entity.ReviewEntity;
import com.luxestay.hotel.repository.BookingRepository;
import com.luxestay.hotel.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;

    @Transactional
    public ReviewDTO createReview(CreateReviewRequest request, Account account) {
        // Validate rating (1-5)
        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        // Get booking
        BookingEntity booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        // Check permission: only the booking owner can review
        if (!booking.getAccount().getId().equals(account.getId())) {
            throw new IllegalStateException("You can only review your own bookings");
        }

        // Anti-spam: check if already reviewed this booking
        if (reviewRepository.existsByBooking_Id(request.getBookingId())) {
            throw new IllegalStateException("You have already reviewed this booking");
        }

        // Check booking status - allow review after booking is confirmed (đã đặt phòng thành công)
        String status = booking.getStatus() != null ? booking.getStatus().toLowerCase() : "";
        // Allow review if booking is confirmed, checked_in, checked_out, or completed
        // Chỉ cần đặt phòng thành công là có thể đánh giá
        if (!status.equals("confirmed") && 
            !status.equals("checked_in") && 
            !status.equals("checked_out") && 
            !status.equals("completed")) {
            throw new IllegalStateException("Bạn chỉ có thể đánh giá sau khi đặt phòng thành công");
        }

        // Create review
        ReviewEntity review = new ReviewEntity();
        review.setBooking(booking);
        review.setRating(request.getRating());
        review.setComment(request.getComment() != null ? request.getComment().trim() : null);
        review.setCreatedAt(LocalDateTime.now());

        review = reviewRepository.save(review);

        // Return DTO
        return toDTO(review);
    }

    public List<ReviewDTO> getReviewsByRoom(Integer roomId) {
        List<ReviewEntity> reviews = reviewRepository.findByRoomId(roomId);
        return reviews.stream().map(this::toDTO).collect(Collectors.toList());
    }

    public RoomRatingDTO getRoomRating(Integer roomId) {
        Double avgRating = reviewRepository.getAverageRatingByRoomId(roomId);
        Long totalReviews = reviewRepository.countByRoomId(roomId);

        // Build histogram
        List<Object[]> histogramData = reviewRepository.getRatingHistogramByRoomId(roomId);
        Map<Integer, Integer> histogram = new HashMap<>();
        for (int i = 5; i >= 1; i--) {
            histogram.put(i, 0); // Initialize all ratings to 0
        }
        for (Object[] row : histogramData) {
            Integer rating = ((Number) row[0]).intValue();
            Integer count = ((Number) row[1]).intValue();
            histogram.put(rating, count);
        }

        return RoomRatingDTO.builder()
                .averageRating(avgRating != null ? avgRating : 0.0)
                .totalReviews(totalReviews != null ? totalReviews.intValue() : 0)
                .ratingHistogram(histogram)
                .build();
    }

    public List<ReviewDTO> getFeaturedReviews(Integer limit) {
        List<ReviewEntity> reviews = reviewRepository.findFeaturedReviews();
        int maxLimit = limit != null && limit > 0 ? limit : 6;
        return reviews.stream()
                .limit(maxLimit)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private ReviewDTO toDTO(ReviewEntity review) {
        BookingEntity booking = review.getBooking();
        Account account = booking.getAccount();

        return ReviewDTO.builder()
                .id(review.getId())
                .bookingId(booking.getId())
                .roomId(booking.getRoom().getId())
                .roomName(booking.getRoom().getRoomName())
                .accountId(account.getId())
                .accountName(account.getFullName())
                .accountAvatar(account.getAvatarUrl())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}


