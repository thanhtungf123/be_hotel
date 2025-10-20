package com.luxestay.hotel.mapper;

import com.luxestay.hotel.model.Room;
import com.luxestay.hotel.model.entity.RoomEntity;

import java.util.Arrays;

public final class RoomMapper {
    private RoomMapper() {
    }

    public static Room toDto(RoomEntity e) {
        Room dto = new Room();
        dto.setId(e.getId() == null ? null : e.getId().longValue());
        dto.setRoomNumber(e.getRoomNumber()); // Map số phòng
        dto.setName(e.getRoomName());
        dto.setType(e.getBedLayout() != null ? e.getBedLayout().getLayoutName() : null);
        dto.setCapacity(e.getCapacity() == null ? 0 : e.getCapacity());
        dto.setSizeSqm(0);
        dto.setPriceVnd(e.getPricePerNight() == null ? 0 : e.getPricePerNight());

        /* 👇 sửa: lấy ảnh từ DB, fallback placeholder */
        String img = e.getImageUrl();
        dto.setImageUrl((img == null || img.isBlank()) ? "/assets/placeholder-room.jpg" : img);

        dto.setPopular(false);

        String am = e.getAmenities();
        dto.setAmenities(am == null ? new String[0]
                : Arrays.stream(am.split("[;,]"))
                        .map(String::trim).filter(s -> !s.isEmpty())
                        .toArray(String[]::new));

        dto.setRating(null);
        dto.setReviews(null);
        dto.setDiscount(null);
        dto.setStatus(e.getStatus()); // Map status field
        dto.setIsVisible(e.getIsVisible() != null ? e.getIsVisible() : true);
        return dto;
    }
}
