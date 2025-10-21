package com.luxestay.hotel.model;

public class Room {
    private Long id;
    private String roomNumber; // Số phòng (101, 102, ...)
    private String name;
    private String type;
    private int capacity;
    private int sizeSqm;
    private int priceVnd;
    private String[] amenities;
    private String imageUrl;
    private boolean popular;

    // +++ phục vụ trang Search
    private Double rating;       // 4.6, 4.7...
    private Integer reviews;     // 156, 124...
    private Integer discount;    // 17 (%), nếu có hiển thị badge
    private String status;       // available, occupied, maintenance
    private Boolean isVisible;   // true = shown in search, false = hidden

    public Room() {}

    public Room(Long id, String name, String type, int capacity, int sizeSqm, int priceVnd,
                String[] amenities, String imageUrl, boolean popular) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.capacity = capacity;
        this.sizeSqm = sizeSqm;
        this.priceVnd = priceVnd;
        this.amenities = amenities;
        this.imageUrl = imageUrl;
        this.popular = popular;
    }

    // getters/setters (đầy đủ)
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public String getRoomNumber() {
        return roomNumber;
    }
    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }

    public int getCapacity() {
        return capacity;
    }
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getSizeSqm() {
        return sizeSqm;
    }
    public void setSizeSqm(int sizeSqm) {
        this.sizeSqm = sizeSqm;
    }

    public int getPriceVnd() {
        return priceVnd;
    }
    public void setPriceVnd(int priceVnd) {
        this.priceVnd = priceVnd;
    }

    public String[] getAmenities() {
        return amenities;
    }
    public void setAmenities(String[] amenities) {
        this.amenities = amenities;
    }

    public String getImageUrl() {
        return imageUrl;
    }
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isPopular() {
        return popular;
    }
    public void setPopular(boolean popular) {
        this.popular = popular;
    }

    public Double getRating() {
        return rating;
    }
    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Integer getReviews() {
        return reviews;
    }
    public void setReviews(Integer reviews) {
        this.reviews = reviews;
    }

    public Integer getDiscount() {
        return discount;
    }
    public void setDiscount(Integer discount) {
        this.discount = discount;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getIsVisible() {
        return isVisible;
    }
    public void setIsVisible(Boolean isVisible) {
        this.isVisible = isVisible;
    }
}
