package com.luxestay.hotel.repository;

import com.luxestay.hotel.model.entity.RoomEntity;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<RoomEntity, Integer>, JpaSpecificationExecutor<RoomEntity> {

      @Query("""
                     SELECT r
                     FROM RoomEntity r
                     LEFT JOIN FETCH r.bedLayout bl
                     WHERE (:statusList IS NULL OR r.status IN :statusList)
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
                  @Param("statusList") List<String> statusList,
                  @Param("layoutNames") List<String> layoutNames,
                  @Param("minPrice") Integer minPrice,
                  @Param("maxPrice") Integer maxPrice,
                  @Param("q") String q,
                  Pageable pageable);

      /**
       * Tìm phòng available trong khoảng thời gian checkIn -> checkOut
       * Phòng available = phòng KHÔNG có booking nào trùng lịch
       * 
       * Booking conflict khi:
       * - checkIn < booking.checkOut AND checkOut > booking.checkIn
       * 
       * Chỉ check các booking có status: pending, confirmed, checked_in
       */
      @Query("""
            SELECT r
            FROM RoomEntity r
            LEFT JOIN FETCH r.bedLayout bl
            WHERE r.status = 'available'
            AND (:minPrice IS NULL OR r.pricePerNight >= :minPrice)
            AND (:maxPrice IS NULL OR r.pricePerNight <= :maxPrice)
            AND (:minCapacity IS NULL OR r.capacity >= :minCapacity)
            AND NOT EXISTS (
            SELECT b
            FROM BookingEntity b
            WHERE b.room.id = r.id
                  AND (
                  LOWER(b.status) IN ('confirmed','checked_in')
                  OR b.paymentState IN ('deposit_paid','paid_in_full')
                  )
                  AND :checkIn < b.checkOut
                  AND :checkOut > b.checkIn
            )
            """)
      Page<RoomEntity> findAvailableRooms(
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("minCapacity") Integer minCapacity,
            @Param("minPrice") Integer minPrice,
            @Param("maxPrice") Integer maxPrice,
            Pageable pageable);

      // Check if room number exists (for validation)
      boolean existsByRoomNumber(String roomNumber);

      /**
       * Đếm số phòng có chứa một amenities cụ thể
       * Chỉ đếm phòng visible và available
       */
      @Query("""
            SELECT COUNT(r)
            FROM RoomEntity r
            WHERE r.isVisible = true
              AND r.status = 'available'
              AND (r.amenities IS NOT NULL)
              AND (
                LOWER(r.amenities) LIKE LOWER(CONCAT('%', :amenity, '%'))
              )
            """)
      Long countRoomsByAmenity(@Param("amenity") String amenity);
}
