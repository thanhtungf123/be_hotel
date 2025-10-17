// com.luxestay.hotel.dto.booking.BookingSummary
package com.luxestay.hotel.dto.booking;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BookingSummary {
    private Integer id;
    private Integer roomId;
    private String  roomName;
    private String  roomImageUrl; // ảnh chính của phòng
    private String  bedLayout;    // ví dụ: "1 giường đôi"
    private Integer guests;       // sức chứa phòng
    private LocalDate checkIn;
    private LocalDate checkOut;
    private Long nights;          // số đêm
    private BigDecimal totalPrice;
    private String status;
    private String cancelReason;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getRoomId() { return roomId; }
    public void setRoomId(Integer roomId) { this.roomId = roomId; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public String getRoomImageUrl() { return roomImageUrl; }
    public void setRoomImageUrl(String roomImageUrl) { this.roomImageUrl = roomImageUrl; }

    public String getBedLayout() { return bedLayout; }
    public void setBedLayout(String bedLayout) { this.bedLayout = bedLayout; }

    public Integer getGuests() { return guests; }
    public void setGuests(Integer guests) { this.guests = guests; }

    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }

    public LocalDate getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }

    public Long getNights() { return nights; }
    public void setNights(Long nights) { this.nights = nights; }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
}
