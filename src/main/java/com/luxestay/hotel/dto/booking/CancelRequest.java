// com.luxestay.hotel.dto.booking.CancelRequest.java
package com.luxestay.hotel.dto.booking;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class CancelRequest {
    private String reason; // tùy chọn
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
