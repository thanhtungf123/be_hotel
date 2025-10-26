package com.luxestay.hotel.dto;

import java.util.List;

/**
 * DTO cho create/update Room
 */
public class RoomRequest {
    private String roomNumber;
    private String roomName;
    private Integer pricePerNight;
    private String description;
    private String amenities; // JSON array string, e.g. "[\"wifi\",\"tv\"]"
    private String status; // available, occupied, maintenance
    private Integer capacity;
    private Integer bedLayoutId;
    private String imageUrl; // Main/primary image URL

    // Optional: list of additional images to add
    private List<RoomImageRequest> images;

    // Getters and Setters
    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public Integer getPricePerNight() {
        return pricePerNight;
    }

    public void setPricePerNight(Integer pricePerNight) {
        this.pricePerNight = pricePerNight;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAmenities() {
        return amenities;
    }

    public void setAmenities(String amenities) {
        this.amenities = amenities;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Integer getBedLayoutId() {
        return bedLayoutId;
    }

    public void setBedLayoutId(Integer bedLayoutId) {
        this.bedLayoutId = bedLayoutId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public List<RoomImageRequest> getImages() {
        return images;
    }

    public void setImages(List<RoomImageRequest> images) {
        this.images = images;
    }
}


