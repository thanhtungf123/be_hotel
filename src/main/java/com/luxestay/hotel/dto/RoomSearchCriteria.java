package com.luxestay.hotel.dto;

import java.util.List;

public class RoomSearchCriteria {
    private Integer guests;
    private Integer priceMax;
    private Integer priceMin; // Giá tối thiểu (thêm mới)
    private List<String> types; // Deluxe, Suite, Presidential, Family
    private List<String> amenities; // WiFi miễn phí, Ban công...
    private List<String> status; // available, occupied, maintenance (🆕 THÊM MỚI)
    private String checkin; // yyyy-MM-dd (để dành)
    private String checkout; // yyyy-MM-dd (để dành)
    private String sort; // priceAsc|priceDesc|ratingDesc
    private Integer page = 0;
    private Integer size = 10;

    public Integer getGuests() {
        return guests;
    }

    public void setGuests(Integer guests) {
        this.guests = guests;
    }

    public Integer getPriceMax() {
        return priceMax;
    }

    public void setPriceMax(Integer priceMax) {
        this.priceMax = priceMax;
    }

    public Integer getPriceMin() {
        return priceMin;
    }

    public void setPriceMin(Integer priceMin) {
        this.priceMin = priceMin;
    }

    public List<String> getTypes() {
        return types;
    }

    public void setTypes(List<String> types) {
        this.types = types;
    }

    public List<String> getAmenities() {
        return amenities;
    }

    public void setAmenities(List<String> amenities) {
        this.amenities = amenities;
    }

    public List<String> getStatus() {
        return status;
    }

    public void setStatus(List<String> status) {
        this.status = status;
    }

    public String getCheckin() {
        return checkin;
    }

    public void setCheckin(String checkin) {
        this.checkin = checkin;
    }

    public String getCheckout() {
        return checkout;
    }

    public void setCheckout(String checkout) {
        this.checkout = checkout;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
