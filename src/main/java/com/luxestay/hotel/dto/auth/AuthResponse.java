package com.luxestay.hotel.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class AuthResponse {
    private String token;     // UUID token (demo). Có thể thay bằng JWT
    private Integer accountId;
    private String fullName;
    private String role;
    private String avatarUrl;

    public AuthResponse(String token, Integer accountId, String fullName, String role) {
        this.token = token;
        this.accountId = accountId;
        this.fullName = fullName;
        this.role = role;
    }

    public AuthResponse(String token, Integer accountId, String fullName, String role, String avatarUrl) {
        this.token = token;
        this.accountId = accountId;
        this.fullName = fullName;
        this.role = role;
        this.avatarUrl = avatarUrl;
    }
}
