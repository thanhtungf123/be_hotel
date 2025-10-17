package com.luxestay.hotel.repository;

import com.luxestay.hotel.model.entity.RoomEntity;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<RoomEntity, Integer>, JpaSpecificationExecutor<RoomEntity> {

    @Query("""
       SELECT r
       FROM RoomEntity r
       LEFT JOIN FETCH r.bedLayout bl
       WHERE (:status IS NULL OR r.status = :status)
         AND (:minPrice IS NULL OR r.pricePerNight >= :minPrice)
         AND (:maxPrice IS NULL OR r.pricePerNight <= :maxPrice)
         AND (
              :layoutNames IS NULL
              OR bl.layoutName IN :layoutNames
         )
         AND (
               :q IS NULL
            OR LOWER(r.roomName)   LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(r.roomNumber) LIKE LOWER(CONCAT('%', :q, '%'))
         )
    """)
    Page<RoomEntity> findForList(
            @Param("status") String status,
            @Param("layoutNames") List<String> layoutNames,
            @Param("minPrice") Integer minPrice,
            @Param("maxPrice") Integer maxPrice,
            @Param("q") String q,
            Pageable pageable
    );
}
