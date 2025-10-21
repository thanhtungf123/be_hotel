package com.luxestay.hotel.repository;

import com.luxestay.hotel.model.entity.BookingEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<BookingEntity, Integer> {

  Optional<BookingEntity> findByIdAndAccount_Id(Integer id, Integer accountId);

  @Query("""
          SELECT b FROM BookingEntity b
          WHERE (:accountId IS NULL OR b.account.id = :accountId)
            AND (:status IS NULL OR LOWER(b.status) = LOWER(:status))
      """)

  Page<BookingEntity> findForHistory(@Param("accountId") Integer accountId,
      @Param("status") String status,
      Pageable pageable);

  // Check if room has active bookings (for status update validation)
  boolean existsByRoom_IdAndStatusInAndCheckOutAfter(
      Integer roomId,
      List<String> statuses,
      LocalDate date);

  // Count bookings by room (for popular rooms recommendation)
  @Query("""
      SELECT b.room.id, COUNT(b.id)
      FROM BookingEntity b
      WHERE b.status IN ('confirmed', 'checked_in', 'checked_out')
      GROUP BY b.room.id
      ORDER BY COUNT(b.id) DESC
      """)
  List<Object[]> countBookingsByRoom();

  // Find user's preferred room types (for personalized recommendation)
  @Query("""
      SELECT b.room.bedLayout.id, COUNT(b.id)
      FROM BookingEntity b
      WHERE b.account.id = :accountId
        AND b.status IN ('confirmed', 'checked_in', 'checked_out')
      GROUP BY b.room.bedLayout.id
      ORDER BY COUNT(b.id) DESC
      """)
  List<Object[]> findUserPreferredRoomTypes(@Param("accountId") Integer accountId);

  // Find all bookings by account ID (for employee service)
  List<BookingEntity> findAllByAccount_Id(Integer accountId);
}
