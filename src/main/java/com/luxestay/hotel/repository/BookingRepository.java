package com.luxestay.hotel.repository;

import com.luxestay.hotel.model.entity.BookingEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BookingRepository extends JpaRepository<BookingEntity, Integer> {

    Optional<BookingEntity> findByIdAndAccount_Id(Integer id, Integer accountId);

    @Query("""
        SELECT b FROM BookingEntity b
        WHERE (:accountId IS NULL OR b.account.id = :accountId)
          AND (:status IS NULL OR LOWER(b.status) = LOWER(:status))
        ORDER BY b.createdAt DESC
    """)
    Page<BookingEntity> findForHistory(@Param("accountId") Integer accountId,
                                       @Param("status") String status,
                                       Pageable pageable);
}
