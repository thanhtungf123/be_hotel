package com.luxestay.hotel.controller;

import com.luxestay.hotel.dto.auth.AuthResponse;
import com.luxestay.hotel.dto.auth.LoginRequest;
import com.luxestay.hotel.dto.auth.RegisterRequest;
import com.luxestay.hotel.model.Account;
import com.luxestay.hotel.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://127.0.0.1:5173",
        "http://localhost:4173",
        "http://localhost:3000"
})
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

    // Lấy thông tin profile của user hiện tại
    @GetMapping("/profile")
    public ResponseEntity<Account> getProfile(@RequestHeader("X-Auth-Token") String token) {
        Account account = authService.requireAccount(token);
        return ResponseEntity.ok(account);
    }

    // Cập nhật profile của user hiện tại
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestHeader("X-Auth-Token") String token,
            @RequestBody Account updatedAccount) {
        Account account = authService.requireAccount(token);
        return ResponseEntity.ok(authService.updateProfile(account.getId(), updatedAccount));
    }

    // Đổi mật khẩu
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestHeader("X-Auth-Token") String token,
            @RequestBody java.util.Map<String, String> request) {
        Account account = authService.requireAccount(token);
        
        String oldPassword = request.get("oldPassword");
        String newPassword = request.get("newPassword");
        
        if (oldPassword == null || newPassword == null) {
            throw new IllegalArgumentException("Thiếu mật khẩu cũ hoặc mới");
        }
        
        authService.changePassword(account.getId(), oldPassword, newPassword);
        return ResponseEntity.ok(java.util.Map.of("message", "Đổi mật khẩu thành công"));
    }

    // Request password reset OTP
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody java.util.Map<String, String> request) {
        String email = request.get("email");
        if (email == null) {
            throw new IllegalArgumentException("Thiếu email");
        }
        
        String otp = authService.requestPasswordReset(email);
        // Don't return OTP in production - it's logged to console for demo
        return ResponseEntity.ok(java.util.Map.of("message", "Mã OTP đã được gửi đến email của bạn"));
    }

    // Verify OTP
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody java.util.Map<String, String> request) {
        String email = request.get("email");
        String otp = request.get("otp");
        
        if (email == null || otp == null) {
            throw new IllegalArgumentException("Thiếu email hoặc mã OTP");
        }
        
        boolean valid = authService.verifyOtp(email, otp);
        if (!valid) {
            throw new IllegalArgumentException("Mã OTP không đúng");
        }
        
        return ResponseEntity.ok(java.util.Map.of("message", "Mã OTP hợp lệ", "verified", true));
    }

    // Reset password after OTP verification
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody java.util.Map<String, String> request) {
        String email = request.get("email");
        String newPassword = request.get("newPassword");
        
        if (email == null || newPassword == null) {
            throw new IllegalArgumentException("Thiếu email hoặc mật khẩu mới");
        }
        
        authService.resetPassword(email, newPassword);
        return ResponseEntity.ok(java.util.Map.of("message", "Đặt lại mật khẩu thành công"));
    }

    // OAuth Login
    @PostMapping("/oauth")
    public ResponseEntity<?> oauthLogin(@RequestBody java.util.Map<String, String> request) {
        String provider = request.get("provider");
        String providerId = request.get("providerId");
        String email = request.get("email");
        String fullName = request.get("fullName");
        String avatarUrl = request.get("avatarUrl");
        
        if (provider == null || providerId == null || email == null) {
            throw new IllegalArgumentException("Thiếu thông tin OAuth");
        }
        
        AuthResponse response = authService.oauthLogin(provider, providerId, email, fullName, avatarUrl);
        return ResponseEntity.ok(response);
    }
}
