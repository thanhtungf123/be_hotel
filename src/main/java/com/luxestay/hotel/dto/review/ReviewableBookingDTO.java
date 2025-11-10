package com.luxestay.hotel.dto.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewableBookingDTO {
    private Integer bookingId;
    private Integer roomId;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private String status;
    private Boolean hasReview; // Đã có review chưa
}

