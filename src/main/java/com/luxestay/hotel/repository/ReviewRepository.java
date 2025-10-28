package com.luxestay.hotel.repository;

import com.luxestay.hotel.model.entity.ReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

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
}
