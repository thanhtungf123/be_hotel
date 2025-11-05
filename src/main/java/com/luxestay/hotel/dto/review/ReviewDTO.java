package com.luxestay.hotel.dto.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewDTO {
    private Integer id;
    private Integer bookingId;
    private Integer roomId;
    private String roomName;
    private Integer accountId;
    private String accountName;
    private String accountAvatar;
    private Integer rating; // 1-5
    private String comment;
    private LocalDateTime createdAt;
}

