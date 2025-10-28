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

  // === Duyệt thanh toán: booking đã có giao dịch (cọc/full), còn pending/confirmed, CHƯA review
  @Query("""
      select b from BookingEntity b
      where b.paymentState in ('deposit_paid','paid_in_full')
        and b.status in ('pending','confirmed')
        and b.paymentReviewedAt is null
      order by b.createdAt desc
      """)
  Page<BookingEntity> findPendingPaymentReviews(Pageable pageable);

  Optional<BookingEntity> findByIdAndAccount_Id(Integer id, Integer accountId);

  @Query("""
      select b from BookingEntity b
      where (:accountId is null or b.account.id = :accountId)
        and (:status is null or lower(b.status) = lower(:status))
      """)
  Page<BookingEntity> findForHistory(@Param("accountId") Integer accountId,
                                     @Param("status") String status,
                                     Pageable pageable);

  BookingEntity findBookingById(Integer bookingId);

  // Check if room has active bookings (for status update validation)
  boolean existsByRoom_IdAndStatusInAndCheckOutAfter(
      Integer roomId,
      List<String> statuses,
      LocalDate date);

<<<<<<< HEAD
     @Query("""
        SELECT COUNT(b) > 0
        FROM BookingEntity b
        WHERE b.room.id = :roomId
                AND (
                LOWER(b.status) IN ('confirmed','checked_in')
                OR b.paymentState IN ('deposit_paid','paid_in_full')
                )
                AND :checkIn < b.checkOut
                AND :checkOut > b.checkIn
        """)
        boolean hasActiveConflict(@Param("roomId") Integer roomId,
                                @Param("checkIn") LocalDate checkIn,
                                @Param("checkOut") LocalDate checkOut);
    Optional<BookingEntity> findAllByAccount_Id(Integer accountId);

    @Query("""
        select b from BookingEntity b
        where b.room.id = :roomId
        and (
        lower(b.status) in ('pending','confirmed','checked_in')
        or b.paymentState in ('deposit_paid','paid_in_full')
        )
        and :start < b.checkOut
        and :end   > b.checkIn
        order by b.checkIn
        """)
        List<BookingEntity> findOverlaps(@Param("roomId") Integer roomId,
                                        @Param("start") LocalDate start,
                                        @Param("end")   LocalDate end);
}
=======
  @Query("""
      SELECT COUNT(b) > 0
      FROM BookingEntity b
      WHERE b.room.id = :roomId
        AND (
          LOWER(b.status) IN ('confirmed','checked_in')
          OR b.paymentState IN ('deposit_paid','paid_in_full')
        )
        AND :checkIn < b.checkOut
        AND :checkOut > b.checkIn
      """)
  boolean hasActiveConflict(@Param("roomId") Integer roomId,
                            @Param("checkIn") LocalDate checkIn,
                            @Param("checkOut") LocalDate checkOut);

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

  // Find all bookings by account ID
  List<BookingEntity> findAllByAccount_Id(Integer accountId);
}
>>>>>>> 1817165665329b070f59038681b3630967f0bf7d
