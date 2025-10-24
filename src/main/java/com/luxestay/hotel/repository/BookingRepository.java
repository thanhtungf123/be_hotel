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

    Optional<BookingEntity> findAllByAccount_Id(Integer accountId);
}