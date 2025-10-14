package com.luxestay.hotel.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class AuthResponse {
    private String token;     // UUID token (demo). Có thể thay bằng JWT
    private Integer accountId;
    private String fullName;
    private String role;
}
