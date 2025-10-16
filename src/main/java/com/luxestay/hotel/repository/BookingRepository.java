// BookingRepository.java
package com.luxestay.hotel.repository;

import com.luxestay.hotel.model.entity.BookingEntity;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BookingRepository extends JpaRepository<BookingEntity, Integer> {

    Optional<BookingEntity> findByIdAndAccount_Id(Integer id, Integer accountId);

    @EntityGraph(attributePaths = {"room", "room.bedLayout"})
    @Query(
            value = """
            SELECT b FROM BookingEntity b
            WHERE (:accountId IS NULL OR b.account.id = :accountId)
              AND (:status IS NULL OR :status = '' OR LOWER(b.status) = LOWER(:status))
            """,
            countQuery = """
            SELECT COUNT(b) FROM BookingEntity b
            WHERE (:accountId IS NULL OR b.account.id = :accountId)
              AND (:status IS NULL OR :status = '' OR LOWER(b.status) = LOWER(:status))
            """
    )
    Page<BookingEntity> findForHistory(@Param("accountId") Integer accountId,
                                       @Param("status") String status,
                                       Pageable pageable);
}
