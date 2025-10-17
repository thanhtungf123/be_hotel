package com.luxestay.hotel.dto.booking;

public class BookingResponse {
    private Integer bookingId;
    private String status;
    private Integer totalVnd;

    public BookingResponse(Integer bookingId, String status, Integer totalVnd) {
        this.bookingId = bookingId;
        this.status = status;
        this.totalVnd = totalVnd;
    }

    public Integer getBookingId() { return bookingId; }
    public String getStatus() { return status; }
    public Integer getTotalVnd() { return totalVnd; }
}
