package com.luxestay.hotel.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rooms")
public class RoomEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Integer id;

    @Column(name = "room_number", length = 10)
    private String roomNumber;

    @Column(name = "room_name", length = 100)
    private String roomName;

    @Column(name = "price_per_night")
    private Integer pricePerNight;

    @Column(name = "description")
    private String description;

    @Column(name = "amenities")
    private String amenities;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bed_layout_id")
    @JsonIgnore
    private BedLayout bedLayout;

    /* 👇 NEW: ảnh đại diện phòng (đồng bộ với ảnh primary trong room_images) */
    @Column(name = "image_url")
    private String imageUrl;

    /* 👇 NEW: visibility flag - hide/show room in search */
    @Column(name = "is_visible", nullable = false)
    private Boolean isVisible = true;

    // getters/setters
    public Integer getId() {
        return id;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public String getRoomName() {
        return roomName;
    }

    public Integer getPricePerNight() {
        return pricePerNight;
    }

    public String getDescription() {
        return description;
    }

    public String getAmenities() {
        return amenities;
    }

    public String getStatus() {
        return status;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public BedLayout getBedLayout() {
        return bedLayout;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public void setPricePerNight(Integer pricePerNight) {
        this.pricePerNight = pricePerNight;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAmenities(String amenities) {
        this.amenities = amenities;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setBedLayout(BedLayout bedLayout) {
        this.bedLayout = bedLayout;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Boolean getIsVisible() {
        return isVisible;
    }

    public void setIsVisible(Boolean isVisible) {
        this.isVisible = isVisible;
    }
}
