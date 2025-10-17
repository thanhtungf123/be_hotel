package com.luxestay.hotel.service;

import com.luxestay.hotel.dto.PagedResponse;
import com.luxestay.hotel.dto.RoomDetail;
import com.luxestay.hotel.dto.RoomSearchCriteria;
import com.luxestay.hotel.model.Room;
import com.luxestay.hotel.dto.RoomImageRequest;

import java.util.List;

public interface RoomService {
    List<Room> listRooms();

    PagedResponse<Room> search(RoomSearchCriteria c);

    RoomDetail getDetail(Long id);

    List<String> addImages(Long roomId, List<RoomImageRequest> images); // thêm/đặt ảnh

    void setPrimaryImage(Long roomId, Integer imageId); // đổi ảnh chính

    void deleteImage(Long roomId, Integer imageId); // xoá ảnh
}
