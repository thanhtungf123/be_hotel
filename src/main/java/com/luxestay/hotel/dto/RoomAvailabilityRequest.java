package com.luxestay.hotel.dto;

import java.time.LocalDate;

/**
 * DTO để kiểm tra phòng có available trong khoảng thời gian không
 */
public class RoomAvailabilityRequest {
    private LocalDate checkIn;
    private LocalDate checkOut;
    private Integer guests;
    private Integer priceMin;
    private Integer priceMax;

    // Getters and Setters
    public LocalDate getCheckIn() {
        return checkIn;
    }

    public void setCheckIn(LocalDate checkIn) {
        this.checkIn = checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public void setCheckOut(LocalDate checkOut) {
        this.checkOut = checkOut;
    }

    public Integer getGuests() {
        return guests;
    }

    public void setGuests(Integer guests) {
        this.guests = guests;
    }

    public Integer getPriceMin() {
        return priceMin;
    }

    public void setPriceMin(Integer priceMin) {
        this.priceMin = priceMin;
    }

    public Integer getPriceMax() {
        return priceMax;
    }

    public void setPriceMax(Integer priceMax) {
        this.priceMax = priceMax;
    }
}





