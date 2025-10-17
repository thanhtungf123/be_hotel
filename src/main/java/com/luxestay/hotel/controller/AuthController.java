package com.luxestay.hotel.controller;

import com.luxestay.hotel.dto.auth.AuthResponse;
import com.luxestay.hotel.dto.auth.LoginRequest;
import com.luxestay.hotel.dto.auth.RegisterRequest;
import com.luxestay.hotel.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
// Đăng ký tài khoản mới
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req));
    }

    // Đăng nhập
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    // Đăng xuất
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("X-Auth-Token") String token) {
        authService.logout(token);
        return ResponseEntity.noContent().build();
    }
}


// Dòng này test merge request