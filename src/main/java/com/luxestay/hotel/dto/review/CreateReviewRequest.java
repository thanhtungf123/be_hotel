package com.luxestay.hotel.dto.review;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateReviewRequest {
    private Integer bookingId;
    private Integer rating; // 1-5
    private String comment;
}
