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

  // Booking đã thanh toán (cọc/full), còn pending/confirmed, CHƯA review
  @Query("""
      select b from BookingEntity b
      where b.paymentState in ('deposit_paid','paid_in_full')
        and b.status in ('pending','confirmed')
        and b.paymentReviewedAt is null
      order by b.createdAt desc
      """)
  Page<BookingEntity> findPendingPaymentReviews(Pageable pageable);

  Optional<BookingEntity> findByIdAndAccount_Id(Integer id, Integer accountId);

  @EntityGraph(attributePaths = {"services"})
  @Query("""
      select b from BookingEntity b
      where (:accountId is null or b.account.id = :accountId)
        and (:status is null or lower(b.status) = lower(:status))
      """)
  Page<BookingEntity> findForHistory(@Param("accountId") Integer accountId,
                                     @Param("status") String status,
                                     Pageable pageable);

  // Find by ID with services eager loaded (for detail views)
  @EntityGraph(attributePaths = {"services"})
  @Query("select b from BookingEntity b where b.id = :id")
  Optional<BookingEntity> findByIdWithServices(@Param("id") Integer id);
  
  // Override findAll to eager load services
  @EntityGraph(attributePaths = {"services"})
  @Override
  Page<BookingEntity> findAll(Pageable pageable);

  BookingEntity findBookingById(Integer bookingId);

  // Check if room has active bookings (for status update validation)
  boolean existsByRoom_IdAndStatusInAndCheckOutAfter(
      Integer roomId,
      List<String> statuses,
      LocalDate date);

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

  // Thống kê số booking theo phòng
  @Query("""
      SELECT b.room.id, COUNT(b.id)
      FROM BookingEntity b
      WHERE b.status IN ('confirmed', 'checked_in', 'checked_out')
      GROUP BY b.room.id
      ORDER BY COUNT(b.id) DESC
      """)
  List<Object[]> countBookingsByRoom();

  // Sở thích loại phòng của user
  @Query("""
      SELECT b.room.bedLayout.id, COUNT(b.id)
      FROM BookingEntity b
      WHERE b.account.id = :accountId
        AND b.status IN ('confirmed', 'checked_in', 'checked_out')
      GROUP BY b.room.bedLayout.id
      ORDER BY COUNT(b.id) DESC
      """)
  List<Object[]> findUserPreferredRoomTypes(@Param("accountId") Integer accountId);

  // Tất cả booking theo account
  List<BookingEntity> findAllByAccount_Id(Integer accountId);

  // Lấy tất cả bookings active của 1 phòng
  @Query("""
      select b from BookingEntity b
      where b.room.id = :roomId
        and (
          lower(b.status) in ('confirmed','checked_in')
          or b.paymentState in ('deposit_paid','paid_in_full')
        )
      order by b.checkIn
      """)
  List<BookingEntity> findActiveBookingsByRoom(@Param("roomId") Integer roomId);

  // Lấy các cancelled bookings có refund info nhưng chưa completed
  @Query("""
      select b from BookingEntity b
      where lower(b.status) = 'cancelled'
        and b.refundSubmittedAt is not null
        and b.refundCompletedAt is null
      order by b.refundSubmittedAt desc, b.createdAt desc
      """)
  Page<BookingEntity> findRefundPendingBookings(Pageable pageable);

  // Lấy các bookings có status cancel_requested hoặc cancelled
  @Query("""
      select b from BookingEntity b
      where lower(b.status) in ('cancel_requested', 'cancelled')
      order by b.createdAt desc
      """)
  Page<BookingEntity> findCancelRequestsAndCancelled(Pageable pageable);

  // ✅ Lấy bookings của user cho một phòng có thể review
  // (confirmed/checked_in/checked_out/completed và chưa có review)
  @Query("""
      select b from BookingEntity b
      where b.account.id = :accountId
        and b.room.id = :roomId
        and lower(b.status) in ('confirmed', 'checked_in', 'checked_out', 'completed')
      order by b.checkOut desc, b.createdAt desc
      """)
  List<BookingEntity> findReviewableBookingsByUserAndRoom(
      @Param("accountId") Integer accountId,
      @Param("roomId") Integer roomId);
}
