package com.luxestay.hotel.dto.review;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomRatingDTO {
    private Double averageRating; // 0.0 - 5.0
    private Integer totalReviews;
    private Map<Integer, Integer> ratingHistogram; // 5->count, 4->count, ...
}

