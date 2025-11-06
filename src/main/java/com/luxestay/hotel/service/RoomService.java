package com.luxestay.hotel.service;

import com.luxestay.hotel.dto.PagedResponse;
import com.luxestay.hotel.dto.RoomAvailabilityRequest;
import com.luxestay.hotel.dto.RoomDetail;
import com.luxestay.hotel.dto.RoomRecommendRequest;
import com.luxestay.hotel.dto.RoomRequest;
import com.luxestay.hotel.dto.RoomSearchCriteria;
import com.luxestay.hotel.model.Room;
import com.luxestay.hotel.dto.RoomImageRequest;

import java.util.List;

public interface RoomService {
    List<Room> listRooms();

    List<Room> listAllRoomsForAdmin(); // List all rooms including hidden ones (admin only)

    PagedResponse<Room> search(RoomSearchCriteria c);

    PagedResponse<Room> checkAvailability(RoomAvailabilityRequest req); // Kiểm tra phòng trống theo ngày

    RoomDetail getDetail(Long id);

    // Admin CRUD operations
    Room createRoom(RoomRequest req); // Tạo phòng mới (admin only)

    Room updateRoom(Long id, RoomRequest req); // Cập nhật phòng (admin only)

    void deleteRoom(Long id); // Xóa phòng (soft delete, admin only)

    void toggleVisibility(Long id, Boolean isVisible); // Toggle show/hide room (admin only)

    void updateRoomStatus(Long id, String newStatus, String reason); // Update room status with state machine validation
                                                                     // (admin only)

    // Recommendation
    List<Room> recommendRooms(RoomRecommendRequest req); // Gợi ý phòng dựa trên thuật toán

    // Image management
    List<String> addImages(Long roomId, List<RoomImageRequest> images); // thêm/đặt ảnh

    void setPrimaryImage(Long roomId, Integer imageId); // đổi ảnh chính

    void deleteImage(Long roomId, Integer imageId); // xoá ảnh

    // Statistics
    java.util.Map<String, Long> getAmenityCounts(); // Đếm số phòng theo từng amenities
}
