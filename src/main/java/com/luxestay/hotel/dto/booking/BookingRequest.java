package com.luxestay.hotel.dto.booking;

public class BookingRequest {
    private Long roomId;
    private String checkIn;   // yyyy-MM-dd
    private String checkOut;  // yyyy-MM-dd
    private Integer guests;
    private String note;

    public Long getRoomId() { return roomId; }
    public String getCheckIn() { return checkIn; }
    public String getCheckOut() { return checkOut; }
    public Integer getGuests() { return guests; }
    public String getNote() { return note; }

    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public void setCheckIn(String checkIn) { this.checkIn = checkIn; }
    public void setCheckOut(String checkOut) { this.checkOut = checkOut; }
    public void setGuests(Integer guests) { this.guests = guests; }
    public void setNote(String note) { this.note = note; }
}
