package com.luxestay.hotel.dto;

import lombok.Data;

@Data
public class RoomStatusUpdateRequest {
    private String status; // available, occupied, maintenance
    private String reason; // Optional: lý do thay đổi (cho maintenance)
}


