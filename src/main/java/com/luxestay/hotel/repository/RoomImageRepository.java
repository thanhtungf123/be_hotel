// com.luxestay.hotel.repository.RoomImageRepository
package com.luxestay.hotel.repository;

import com.luxestay.hotel.model.entity.RoomImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RoomImageRepository extends JpaRepository<RoomImage, Integer> {
    List<RoomImage> findByRoom_IdOrderByIsPrimaryDescSortOrderAsc(Integer roomId);
    void deleteByRoom_Id(Integer roomId);
}
