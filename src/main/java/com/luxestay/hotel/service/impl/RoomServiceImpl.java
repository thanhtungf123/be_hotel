package com.luxestay.hotel.service.impl;

import com.luxestay.hotel.dto.PagedResponse;
import com.luxestay.hotel.dto.RoomAvailabilityRequest;
import com.luxestay.hotel.dto.RoomDetail;
import com.luxestay.hotel.dto.RoomImageRequest;
import com.luxestay.hotel.dto.RoomRecommendRequest;
import com.luxestay.hotel.dto.RoomRequest;
import com.luxestay.hotel.dto.RoomSearchCriteria;
import com.luxestay.hotel.mapper.RoomMapper;
import com.luxestay.hotel.model.Review;
import com.luxestay.hotel.model.Room;
import com.luxestay.hotel.model.entity.BedLayout;
import com.luxestay.hotel.model.entity.RoomEntity;
import com.luxestay.hotel.model.entity.RoomImage;
import com.luxestay.hotel.repository.BedLayoutRepository;
import com.luxestay.hotel.repository.BookingRepository;
import com.luxestay.hotel.repository.RoomImageRepository;
import com.luxestay.hotel.repository.RoomRepository;
import com.luxestay.hotel.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final RoomImageRepository roomImageRepository;
    private final BedLayoutRepository bedLayoutRepository;
    private final BookingRepository bookingRepository;

    @Override
    public List<Room> listRooms() {
        // Chỉ hiển thị phòng available và visible cho trang chủ
        Page<RoomEntity> page = roomRepository.findForList(
                List.of("available"), // chỉ lấy phòng available
                null, // layoutNames
                null, // minPrice
                null, // maxPrice
                null, // q
                PageRequest.of(0, 50, Sort.by("pricePerNight").ascending()));

        // Filter only visible rooms
        return page.getContent().stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsVisible()))
                .map(RoomMapper::toDto)
                .toList();
    }

    @Override
    public List<Room> listAllRoomsForAdmin() {
        // Return ALL rooms (including hidden ones) for admin panel
        Page<RoomEntity> page = roomRepository.findForList(
                List.of("available", "occupied", "maintenance"), // statusList - all statuses
                null, // layoutNames
                null, // minPrice
                null, // maxPrice
                null, // q (search query)
                PageRequest.of(0, 100, Sort.by("id").ascending())); // Get all, sorted by ID

        // Return ALL rooms without filtering by isVisible
        return page.getContent().stream()
                .map(RoomMapper::toDto)
                .toList();
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

        // ⇩ Lấy danh sách status (available, occupied, maintenance)
        List<String> statusList = (c.getStatus() == null || c.getStatus().isEmpty())
                ? null
                : c.getStatus();

        // Nếu bạn có ô search text riêng thì gán cho q, còn ở đây không dùng:
        String q = null;

        Page<RoomEntity> page = roomRepository.findForList(
                statusList, // 🆕 lọc theo status
                layoutNames, // lọc theo bed_layouts.layout_name (IN)
                c.getPriceMin(), // 🆕 minPrice
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

        // Filter only visible rooms
        list = list.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsVisible()))
                .toList();

        List<Room> items = list.stream().map(RoomMapper::toDto).toList();
        return new PagedResponse<>(items, (int) page.getTotalElements(), page.getNumber(), page.getSize());
    }

    @Override
    public PagedResponse<Room> checkAvailability(RoomAvailabilityRequest req) {
        // Validate dates
        if (req.getCheckIn() == null || req.getCheckOut() == null) {
            throw new IllegalArgumentException("CheckIn và CheckOut không được để trống");
        }
        if (req.getCheckIn().isAfter(req.getCheckOut())) {
            throw new IllegalArgumentException("CheckIn phải trước CheckOut");
        }
        if (req.getCheckIn().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("CheckIn phải từ hôm nay trở đi");
        }

        Pageable pageable = PageRequest.of(0, 50, Sort.by("pricePerNight").ascending());

        Page<RoomEntity> page = roomRepository.findAvailableRooms(
                req.getCheckIn(),
                req.getCheckOut(),
                req.getGuests(),
                req.getPriceMin(),
                req.getPriceMax(),
                pageable);

        // Filter only visible rooms
        List<Room> items = page.getContent().stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsVisible()))
                .map(RoomMapper::toDto)
                .toList();
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
     * =================================================================
     * ====== ADMIN CRUD OPERATIONS ======
     * =================================================================
     */

    @Override
    @Transactional
    public Room createRoom(RoomRequest req) {
        // Validate required fields
        if (req.getRoomNumber() == null || req.getRoomNumber().isBlank()) {
            throw new IllegalArgumentException("Room number is required");
        }
        if (req.getRoomName() == null || req.getRoomName().isBlank()) {
            throw new IllegalArgumentException("Room name is required");
        }
        if (req.getPricePerNight() == null || req.getPricePerNight() <= 0) {
            throw new IllegalArgumentException("Valid price is required");
        }

        // Check if room number already exists
        if (roomRepository.existsByRoomNumber(req.getRoomNumber())) {
            throw new IllegalArgumentException("Room number already exists");
        }

        RoomEntity entity = new RoomEntity();
        entity.setRoomNumber(req.getRoomNumber());
        entity.setRoomName(req.getRoomName());
        entity.setPricePerNight(req.getPricePerNight());
        entity.setDescription(req.getDescription());
        entity.setAmenities(req.getAmenities());
        entity.setStatus(req.getStatus() != null ? req.getStatus() : "available");
        entity.setCapacity(req.getCapacity() != null ? req.getCapacity() : 2);
        entity.setImageUrl(req.getImageUrl());
        entity.setCreatedAt(LocalDateTime.now());

        // Set bed layout if provided
        if (req.getBedLayoutId() != null) {
            BedLayout bedLayout = bedLayoutRepository.findById(req.getBedLayoutId())
                    .orElseThrow(() -> new IllegalArgumentException("Bed layout not found"));
            entity.setBedLayout(bedLayout);
        }

        entity = roomRepository.save(entity);

        // Add images if provided
        if (req.getImages() != null && !req.getImages().isEmpty()) {
            addImages(Long.valueOf(entity.getId()), req.getImages());
        }

        return RoomMapper.toDto(entity);
    }

    @Override
    @Transactional
    public Room updateRoom(Long id, RoomRequest req) {
        RoomEntity entity = roomRepository.findById(id.intValue())
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        // Update fields if provided
        if (req.getRoomNumber() != null && !req.getRoomNumber().isBlank()) {
            // Check if new room number conflicts with another room
            if (!entity.getRoomNumber().equals(req.getRoomNumber()) &&
                    roomRepository.existsByRoomNumber(req.getRoomNumber())) {
                throw new IllegalArgumentException("Room number already exists");
            }
            entity.setRoomNumber(req.getRoomNumber());
        }

        if (req.getRoomName() != null && !req.getRoomName().isBlank()) {
            entity.setRoomName(req.getRoomName());
        }

        if (req.getPricePerNight() != null) {
            if (req.getPricePerNight() <= 0) {
                throw new IllegalArgumentException("Price must be positive");
            }
            entity.setPricePerNight(req.getPricePerNight());
        }

        if (req.getDescription() != null) {
            entity.setDescription(req.getDescription());
        }

        if (req.getAmenities() != null) {
            entity.setAmenities(req.getAmenities());
        }

        if (req.getStatus() != null) {
            entity.setStatus(req.getStatus());
        }

        if (req.getCapacity() != null) {
            entity.setCapacity(req.getCapacity());
        }

        if (req.getImageUrl() != null) {
            entity.setImageUrl(req.getImageUrl());
        }

        if (req.getBedLayoutId() != null) {
            BedLayout bedLayout = bedLayoutRepository.findById(req.getBedLayoutId())
                    .orElseThrow(() -> new IllegalArgumentException("Bed layout not found"));
            entity.setBedLayout(bedLayout);
        }

        entity = roomRepository.save(entity);

        // Update images if provided
        if (req.getImages() != null && !req.getImages().isEmpty()) {
            addImages(id, req.getImages());
        }

        return RoomMapper.toDto(entity);
    }

    @Override
    @Transactional
    public void deleteRoom(Long id) {
        RoomEntity entity = roomRepository.findById(id.intValue())
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        // Soft delete - set status to 'deleted' or 'inactive'
        entity.setStatus("deleted");
        roomRepository.save(entity);
    }

    @Override
    @Transactional
    public void toggleVisibility(Long id, Boolean isVisible) {
        RoomEntity entity = roomRepository.findById(id.intValue())
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        // Toggle visibility
        entity.setIsVisible(isVisible);
        roomRepository.save(entity);
    }

    /*
     * ====== Phần quản lý ảnh (add / setPrimary / delete) giữ nguyên logic của bạn
     * ======
     */

    @Override
    public List<String> addImages(Long roomId, List<RoomImageRequest> images) {
        var room = roomRepository.findById(roomId.intValue())
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        if (images == null || images.isEmpty()) {
            return List.of();
        }

        boolean hasPrimaryInRequest = images.stream()
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

    @Override
    @Transactional
    public void updateRoomStatus(Long id, String newStatus, String reason) {
        // Validate status values
        if (!newStatus.matches("available|occupied|maintenance")) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ: " + newStatus);
        }

        // Find room
        RoomEntity room = roomRepository.findById(id.intValue())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng với ID: " + id));

        String currentStatus = room.getStatus();

        // If same status, no change needed
        if (currentStatus.equals(newStatus)) {
            return;
        }

        // Validate state transitions based on business rules
        validateStatusTransition(room, currentStatus, newStatus);

        // Update status
        room.setStatus(newStatus);
        roomRepository.save(room);
    }

    private void validateStatusTransition(RoomEntity room, String currentStatus, String newStatus) {
        switch (currentStatus) {
            case "available":
                // Available can go to: occupied (via booking) or maintenance (admin)
                if (newStatus.equals("occupied")) {
                    // Check if there's an active booking for this room
                    boolean hasActiveBooking = hasActiveBooking(room.getId());
                    if (!hasActiveBooking) {
                        throw new IllegalArgumentException(
                                "Không thể chuyển sang 'occupied': Phòng chưa có booking đang hoạt động. " +
                                        "Trạng thái 'occupied' được tự động cập nhật khi có khách check-in.");
                    }
                }
                // Allow: available → maintenance
                break;

            case "occupied":
                // Occupied can only go to: available (after checkout)
                // Cannot go directly to maintenance without checkout
                if (newStatus.equals("maintenance")) {
                    throw new IllegalArgumentException(
                            "Không thể chuyển trực tiếp từ 'occupied' sang 'maintenance'. " +
                                    "Vui lòng chờ khách checkout (trạng thái available) trước.");
                }
                // Allow: occupied → available
                break;

            case "maintenance":
                // Maintenance can go to: available
                // Cannot go directly to occupied
                if (newStatus.equals("occupied")) {
                    throw new IllegalArgumentException(
                            "Không thể chuyển trực tiếp từ 'maintenance' sang 'occupied'. " +
                                    "Vui lòng đặt về 'available' trước, sau đó khách có thể booking.");
                }
                // Allow: maintenance → available
                break;

            default:
                throw new IllegalArgumentException("Trạng thái hiện tại không hợp lệ: " + currentStatus);
        }
    }

    @Override
    public List<Room> recommendRooms(RoomRecommendRequest req) {
        String type = req.getType() != null ? req.getType().toLowerCase() : "auto";
        int limit = req.getLimit() != null && req.getLimit() > 0 ? req.getLimit() : 5;

        List<RoomEntity> recommendedRooms = new ArrayList<>();

        switch (type) {
            case "popular":
                recommendedRooms = getTopBookedRooms(limit);
                break;
            case "top_rated":
                recommendedRooms = getTopRatedRooms(limit);
                break;
            case "personalized":
                if (req.getAccountId() != null) {
                    recommendedRooms = getPersonalizedRooms(req.getAccountId().intValue(), limit);
                }
                break;
            case "auto":
            default:
                // Auto: Try personalized first, fallback to popular, then top rated, finally
                // available
                if (req.getAccountId() != null) {
                    recommendedRooms = getPersonalizedRooms(req.getAccountId().intValue(), limit);
                }
                if (recommendedRooms.isEmpty()) {
                    recommendedRooms = getTopBookedRooms(limit);
                }
                if (recommendedRooms.isEmpty()) {
                    recommendedRooms = getTopRatedRooms(limit);
                }
                break;
        }

        // Fallback to available rooms if no recommendations found
        if (recommendedRooms.isEmpty()) {
            recommendedRooms = getAvailableRooms(limit);
        }

        // Filter only visible rooms and convert to DTO
        return recommendedRooms.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsVisible()))
                .limit(limit)
                .map(RoomMapper::toDto)
                .collect(Collectors.toList());
    }

    private List<RoomEntity> getTopBookedRooms(int limit) {
        try {
            List<Object[]> stats = bookingRepository.countBookingsByRoom();
            if (stats.isEmpty()) {
                return new ArrayList<>();
            }

            // Extract room IDs sorted by booking count
            List<Integer> topRoomIds = stats.stream()
                    .limit(limit * 2) // Get more than needed in case some are hidden
                    .map(row -> (Integer) row[0])
                    .collect(Collectors.toList());

            if (topRoomIds.isEmpty()) {
                return new ArrayList<>();
            }

            // Fetch rooms by IDs
            return roomRepository.findAllById(topRoomIds).stream()
                    .filter(r -> "available".equals(r.getStatus()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error getting top booked rooms: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<RoomEntity> getTopRatedRooms(int limit) {
        // For now, return empty - will implement when ReviewRepository is properly
        // integrated
        // This requires joining reviews with bookings and rooms
        return new ArrayList<>();
    }

    private List<RoomEntity> getPersonalizedRooms(Integer accountId, int limit) {
        try {
            // Find user's preferred room types (bed layouts) based on booking history
            List<Object[]> preferences = bookingRepository.findUserPreferredRoomTypes(accountId);
            if (preferences.isEmpty()) {
                return new ArrayList<>();
            }

            // Get the most preferred bed layout ID
            Integer preferredLayoutId = (Integer) preferences.get(0)[0];

            // Find available rooms with that bed layout
            Page<RoomEntity> rooms = roomRepository.findForList(
                    List.of("available"),
                    null, // layoutNames (we'll filter by ID below)
                    null, // minPrice
                    null, // maxPrice
                    null, // q
                    PageRequest.of(0, limit * 2, Sort.by("pricePerNight").ascending()));

            return rooms.getContent().stream()
                    .filter(r -> r.getBedLayout() != null && r.getBedLayout().getId().equals(preferredLayoutId))
                    .limit(limit)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error getting personalized rooms: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<RoomEntity> getAvailableRooms(int limit) {
        // Fallback: Return available rooms sorted by price
        Page<RoomEntity> page = roomRepository.findForList(
                List.of("available"),
                null, // layoutNames
                null, // minPrice
                null, // maxPrice
                null, // q
                PageRequest.of(0, limit, Sort.by("pricePerNight").ascending()));

        return page.getContent();
    }

    private boolean hasActiveBooking(Integer roomId) {
        // Check if there's any booking with status: confirmed or checked_in for this
        // room
        // that covers today or future dates
        LocalDate today = LocalDate.now();

        // Query bookings for this room with active statuses
        return bookingRepository.existsByRoom_IdAndStatusInAndCheckOutAfter(
                roomId,
                List.of("confirmed", "checked_in"),
                today);
    }
}
