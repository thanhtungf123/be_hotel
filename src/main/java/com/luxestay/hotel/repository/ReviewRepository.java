package com.luxestay.hotel.repository;

import com.luxestay.hotel.model.entity.ReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Integer> {

    // Get average rating per room (for recommendation: top rated)
    @Query(value = """
            SELECT r.room_id, CAST(AVG(CAST(rv.rating AS FLOAT)) AS DECIMAL(3,2)) as avg_rating
            FROM reviews rv
            JOIN bookings b ON rv.booking_id = b.booking_id
            JOIN rooms r ON b.room_id = r.room_id
            GROUP BY r.room_id
            ORDER BY avg_rating DESC
            """, nativeQuery = true)
    List<Object[]> findAvgRatingByRoom();

    // Get reviews by room ID
    @Query("""
        SELECT r FROM ReviewEntity r
        WHERE r.booking.room.id = :roomId
        ORDER BY r.createdAt DESC
        """)
    List<ReviewEntity> findByRoomId(@Param("roomId") Integer roomId);

    // Check if user already reviewed a booking (anti-spam)
    boolean existsByBooking_Id(Integer bookingId);

    // Get review by booking ID (to check if already exists)
    Optional<ReviewEntity> findByBooking_Id(Integer bookingId);

    // Get average rating for a specific room
    @Query(value = """
        SELECT CAST(AVG(CAST(rating AS FLOAT)) AS DECIMAL(3,2))
        FROM reviews rv
        JOIN bookings b ON rv.booking_id = b.booking_id
        WHERE b.room_id = :roomId
        """, nativeQuery = true)
    Double getAverageRatingByRoomId(@Param("roomId") Integer roomId);

    // Get rating histogram for a room (count per rating 1-5)
    @Query(value = """
        SELECT rv.rating, COUNT(*) as count
        FROM reviews rv
        JOIN bookings b ON rv.booking_id = b.booking_id
        WHERE b.room_id = :roomId
        GROUP BY rv.rating
        ORDER BY rv.rating DESC
        """, nativeQuery = true)
    List<Object[]> getRatingHistogramByRoomId(@Param("roomId") Integer roomId);

    // Count total reviews for a room
    @Query("""
        SELECT COUNT(r) FROM ReviewEntity r
        WHERE r.booking.room.id = :roomId
        """)
    Long countByRoomId(@Param("roomId") Integer roomId);

    // ✅ Featured reviews (rating >= 4, có comment, mới nhất trước)
    @Query("""
        SELECT r FROM ReviewEntity r
        WHERE r.rating >= 4 AND r.comment IS NOT NULL AND LENGTH(TRIM(r.comment)) > 0
        ORDER BY r.rating DESC, r.createdAt DESC
        """)
    List<ReviewEntity> findFeaturedReviews();
}
