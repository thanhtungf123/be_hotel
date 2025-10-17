package com.luxestay.hotel.service.impl;

import com.luxestay.hotel.dto.PagedResponse;
import com.luxestay.hotel.dto.RoomDetail;
import com.luxestay.hotel.dto.RoomImageRequest;
import com.luxestay.hotel.dto.RoomSearchCriteria;
import com.luxestay.hotel.mapper.RoomMapper;
import com.luxestay.hotel.model.Review;
import com.luxestay.hotel.model.Room;
import com.luxestay.hotel.model.entity.RoomEntity;
import com.luxestay.hotel.model.entity.RoomImage;
import com.luxestay.hotel.repository.RoomImageRepository;
import com.luxestay.hotel.repository.RoomRepository;
import com.luxestay.hotel.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;

    /* 👇 NEW: tiêm repository ảnh để dùng thật */
    private final RoomImageRepository roomImageRepository;

    @Override
    public List<Room> listRooms() {
        Page<RoomEntity> page = roomRepository.findForList(
                null, null, null, null, null,
                PageRequest.of(0, 50, Sort.by("pricePerNight").ascending()));
        return page.getContent().stream().map(RoomMapper::toDto).toList();
    }

    @Override
    public PagedResponse<Room> search(RoomSearchCriteria c) {
        Sort sort = switch (String.valueOf(c.getSort()).toLowerCase()) {
            case "pricedesc" -> Sort.by("pricePerNight").descending();
            case "ratingdesc" -> Sort.by("pricePerNight").ascending(); // tạm
            default -> Sort.by("pricePerNight").ascending();
        };

        Pageable pageable = PageRequest.of(
                Optional.ofNullable(c.getPage()).orElse(0),
                Optional.ofNullable(c.getSize()).orElse(10),
                sort);

        // ⇩ Lấy danh sách bed layout người dùng chọn (nếu rỗng => null)
        List<String> layoutNames = (c.getTypes() == null || c.getTypes().isEmpty())
                ? null
                : c.getTypes();

        // Nếu bạn có ô search text riêng thì gán cho q, còn ở đây không dùng:
        String q = null;

        Page<RoomEntity> page = roomRepository.findForList(
                null, // status
                layoutNames, // lọc theo bed_layouts.layout_name (IN)
                null, // minPrice (nếu cần có thể thêm từ UI)
                c.getPriceMax(), // maxPrice
                q, // q
                pageable);

        List<RoomEntity> list = new ArrayList<>(page.getContent());

        // lọc capacity theo guests
        if (c.getGuests() != null) {
            list = list.stream()
                    .filter(r -> r.getCapacity() != null && r.getCapacity() >= c.getGuests())
                    .toList();
        }

        // lọc amenities (ít nhất 1 trùng)
        if (c.getAmenities() != null && !c.getAmenities().isEmpty()) {
            Set<String> need = c.getAmenities().stream()
                    .map(s -> s.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
            list = list.stream().filter(r -> {
                String am = Optional.ofNullable(r.getAmenities()).orElse("");
                for (String token : am.split("[;,]")) {
                    if (need.contains(token.trim().toLowerCase(Locale.ROOT)))
                        return true;
                }
                return false;
            }).toList();
        }

        List<Room> items = list.stream().map(RoomMapper::toDto).toList();
        return new PagedResponse<>(items, (int) page.getTotalElements(), page.getNumber(), page.getSize());
    }

    @Override
    public RoomDetail getDetail(Long id) {
        RoomEntity e = roomRepository.findById(id.intValue())
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        Room room = RoomMapper.toDto(e);
        room.setRating(4.7);
        room.setReviews(120);

        /* 👇 NEW: lấy gallery từ DB; nếu rỗng thì fallback ảnh đại diện/placeholder */
        List<RoomImage> imgs = roomImageRepository
                .findByRoom_IdOrderByIsPrimaryDescSortOrderAsc(e.getId());
        List<String> gallery;
        if (!imgs.isEmpty()) {
            gallery = imgs.stream().map(RoomImage::getImageUrl).toList();
            room.setImageUrl(gallery.get(0)); // đồng bộ ảnh đại diện theo ảnh primary
        } else if (room.getImageUrl() != null && !room.getImageUrl().isBlank()) {
            gallery = List.of(room.getImageUrl());
        } else {
            gallery = List.of("/assets/placeholder-room.jpg");
            room.setImageUrl(gallery.get(0));
        }

        RoomDetail d = new RoomDetail();
        d.setRoom(room);
        d.setFloorRange("Tầng 2-5");
        d.setDescription(Optional.ofNullable(e.getDescription())
                .orElse("Phòng " + room.getName() + " trang bị đầy đủ tiện nghi, phù hợp nghỉ dưỡng/công tác."));
        d.setHighlights(Arrays.asList(room.getAmenities() != null ? room.getAmenities() : new String[0]));
        d.setGallery(gallery);
        d.setAmenities(Map.of(
                "Tiện nghi cơ bản", List.of("WiFi miễn phí", "Điều hòa", "TV", "Két an toàn"),
                "Phòng tắm", List.of("Vòi sen", "Đồ dùng tắm"),
                "Dịch vụ", List.of("Giặt ủi", "Dọn phòng")));
        d.setRatingHistogram(Map.of(5, 78, 4, 32, 3, 10, 2, 3, 1, 1));
        d.setReviews(List.of(
                new Review("Khách Ẩn Danh", "https://i.pravatar.cc/64?img=1", 5, "Phòng sạch đẹp, đúng mô tả.",
                        "2 tuần trước")));
        return d;
    }

    /*
     * ====== Phần quản lý ảnh (add / setPrimary / delete) giữ nguyên logic của bạn
     * ======
     */

    @Override
    public List<String> addImages(Long roomId, List<RoomImageRequest> images) {
        var room = roomRepository.findById(roomId.intValue())
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        boolean hasPrimaryInRequest = images != null && images.stream()
                .anyMatch(x -> Boolean.TRUE.equals(x.getPrimary()));
        if (hasPrimaryInRequest) {
            var exist = roomImageRepository.findByRoom_IdOrderByIsPrimaryDescSortOrderAsc(room.getId());
            exist.forEach(i -> {
                if (Boolean.TRUE.equals(i.getIsPrimary()))
                    i.setIsPrimary(false);
            });
            roomImageRepository.saveAll(exist);
        }

        int autoOrder = 1;
        List<String> gallery = new ArrayList<>();
        for (var req : images) {
            if (req.getImageUrl() == null || req.getImageUrl().isBlank())
                continue;
            var img = new RoomImage();
            img.setRoom(room);
            img.setImageUrl(req.getImageUrl().trim());
            img.setIsPrimary(Boolean.TRUE.equals(req.getPrimary()));
            img.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : autoOrder++);
            roomImageRepository.save(img);
            gallery.add(img.getImageUrl());
            if (img.getIsPrimary())
                room.setImageUrl(img.getImageUrl());
        }
        roomRepository.save(room);
        return gallery;
    }

    @Override
    public void setPrimaryImage(Long roomId, Integer imageId) {
        var room = roomRepository.findById(roomId.intValue())
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));
        var all = roomImageRepository.findByRoom_IdOrderByIsPrimaryDescSortOrderAsc(room.getId());
        all.forEach(i -> i.setIsPrimary(false));
        roomImageRepository.saveAll(all);

        var img = roomImageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found"));
        if (!img.getRoom().getId().equals(room.getId()))
            throw new IllegalArgumentException("Image does not belong to room");

        img.setIsPrimary(true);
        img.setSortOrder(0);
        roomImageRepository.save(img);
        room.setImageUrl(img.getImageUrl());
        roomRepository.save(room);
    }

    @Override
    public void deleteImage(Long roomId, Integer imageId) {
        var img = roomImageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found"));
        if (!img.getRoom().getId().equals(roomId.intValue()))
            throw new IllegalArgumentException("Image does not belong to room");

        boolean wasPrimary = Boolean.TRUE.equals(img.getIsPrimary());
        roomImageRepository.delete(img);

        if (wasPrimary) {
            var remain = roomImageRepository.findByRoom_IdOrderByIsPrimaryDescSortOrderAsc(roomId.intValue());
            if (!remain.isEmpty()) {
                remain.get(0).setIsPrimary(true);
                roomImageRepository.save(remain.get(0));
                var room = roomRepository.findById(roomId.intValue()).orElseThrow();
                room.setImageUrl(remain.get(0).getImageUrl());
                roomRepository.save(room);
            }
        }
    }
}
