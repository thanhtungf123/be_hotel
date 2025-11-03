package com.luxestay.hotel.service;

import com.luxestay.hotel.dto.auth.AuthResponse;
import com.luxestay.hotel.dto.auth.LoginRequest;
import com.luxestay.hotel.dto.auth.RegisterRequest;
import com.luxestay.hotel.model.Account;
import com.luxestay.hotel.model.Role;
import com.luxestay.hotel.repository.AccountRepository;
import com.luxestay.hotel.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // Demo in-memory session tokens (token -> accountId)
    private final Map<String, Integer> sessions = new HashMap<>();

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (accountRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email đã tồn tại");
        }

        Role customerRole = roleRepository.findByName("customer")
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy role 'account'"));

        Account acc = Account.builder()
                .fullName(req.getFullName())
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .phoneNumber(req.getPhoneNumber())
                .role(customerRole)
                .isActive(true)
                .build();

        acc = accountRepository.save(acc);

        String token = UUID.randomUUID().toString();
        sessions.put(token, acc.getId());

        return new AuthResponse(token, acc.getId(), acc.getFullName(), 
                customerRole.getName(), acc.getAvatarUrl());
    }

    public AuthResponse login(LoginRequest req) {
        Account acc = accountRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản không tồn tại"));

        if (!Boolean.TRUE.equals(acc.getIsActive())) {
            throw new IllegalStateException("Tài khoản đã bị khóa");
        }

        if (!passwordEncoder.matches(req.getPassword(), acc.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu không đúng");
        }

        String token = UUID.randomUUID().toString();
        sessions.put(token, acc.getId());
//        sessions.put("role", acc.getRole().);

        return new AuthResponse(token, acc.getId(), acc.getFullName(),
                acc.getRole() != null ? acc.getRole().getName() : null,
                acc.getAvatarUrl());
    }

    public Optional<Integer> verify(String token) {
        return Optional.ofNullable(sessions.get(token));
    }

    public void logout(String token) {
        sessions.remove(token);
    }

    public Account requireAccount(String token) {
        Integer id = verify(token).orElseThrow(() -> new IllegalArgumentException("Bạn cần đăng nhập"));
        return accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản không tồn tại"));
    }
    
    public Optional<Account> getCurrentAccount() {
        // This is a simplified implementation - in a real app, you'd get the token from SecurityContext
        // For now, we'll return empty to indicate no current user
        return Optional.empty();
    }

    @Transactional
    public Account updateProfile(Integer accountId, Account updatedAccount) {
        Account existing = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản không tồn tại"));

        // Chỉ cho phép update một số field nhất định
        if (updatedAccount.getFullName() != null) {
            existing.setFullName(updatedAccount.getFullName());
        }
        if (updatedAccount.getPhoneNumber() != null) {
            existing.setPhoneNumber(updatedAccount.getPhoneNumber());
        }
        if (updatedAccount.getAvatarUrl() != null) {
            existing.setAvatarUrl(updatedAccount.getAvatarUrl());
        }

        return accountRepository.save(existing);
    }

    @Transactional
    public void changePassword(Integer accountId, String oldPassword, String newPassword) {
        Account existing = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Tài khoản không tồn tại"));

        // Verify old password
        if (!passwordEncoder.matches(oldPassword, existing.getPasswordHash())) {
            throw new IllegalArgumentException("Mật khẩu cũ không đúng");
        }

        // Update to new password
        existing.setPasswordHash(passwordEncoder.encode(newPassword));
        accountRepository.save(existing);
    }

    // OTP storage for password reset (in-memory)
    private final Map<String, String> otpStore = new HashMap<>(); // email -> OTP
    private final Map<String, Long> otpExpiry = new HashMap<>();  // email -> expiry timestamp
    private final Map<String, Long> lastOtpSent = new HashMap<>(); // email -> last sent timestamp

    @Transactional
    public String requestPasswordReset(String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email không tồn tại"));

        // Check if last OTP was sent less than 1 minute ago
        Long lastSent = lastOtpSent.get(email);
        if (lastSent != null && System.currentTimeMillis() - lastSent < 60 * 1000) {
            long remainingSeconds = 60 - ((System.currentTimeMillis() - lastSent) / 1000);
            throw new IllegalStateException("Vui lòng đợi " + remainingSeconds + " giây trước khi gửi lại OTP");
        }

        // Generate 6-digit OTP
        String otp = String.format("%06d", new Random().nextInt(999999));
        
        // Store OTP with 10 minutes expiry
        otpStore.put(email, otp);
        otpExpiry.put(email, System.currentTimeMillis() + (10 * 60 * 1000));
        lastOtpSent.put(email, System.currentTimeMillis());
        
        // Send email with OTP
        emailService.sendOtpEmail(email, otp);
        
        // Also log to console for backup
        System.out.println("=== PASSWORD RESET OTP for " + email + " ===");
        System.out.println("OTP: " + otp);
        System.out.println("Valid for 10 minutes");
        System.out.println("================================");
        
        return otp;
    }

    public boolean verifyOtp(String email, String otp) {
        String storedOtp = otpStore.get(email);
        Long expiry = otpExpiry.get(email);
        
        if (storedOtp == null || expiry == null) {
            return false;
        }
        
        if (System.currentTimeMillis() > expiry) {
            otpStore.remove(email);
            otpExpiry.remove(email);
            throw new IllegalStateException("Mã OTP đã hết hạn");
        }
        
        boolean valid = storedOtp.equals(otp);
        if (valid) {
            otpStore.remove(email);
            otpExpiry.remove(email);
        }
        return valid;
    }

    @Transactional
    public void resetPassword(String email, String newPassword) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Email không tồn tại"));

        account.setPasswordHash(passwordEncoder.encode(newPassword));
        accountRepository.save(account);
    }

    // OAuth Login
    @Transactional
    public AuthResponse oauthLogin(String provider, String providerId, String email, String fullName, String avatarUrl) {
        // Try to find existing account by provider and providerId
        Optional<Account> existingAccount = accountRepository.findByProviderAndProviderId(provider, providerId);
        
        Account account;
        if (existingAccount.isPresent()) {
            account = existingAccount.get();
            
            // Update info if changed
            if (fullName != null && !fullName.equals(account.getFullName())) {
                account.setFullName(fullName);
            }
            if (avatarUrl != null && !avatarUrl.equals(account.getAvatarUrl())) {
                account.setAvatarUrl(avatarUrl);
            }
            account = accountRepository.save(account);
        } else {
            // Check if email already exists (but with different provider)
            Optional<Account> emailAccount = accountRepository.findByEmail(email);
            if (emailAccount.isPresent()) {
                throw new IllegalArgumentException("Email đã được sử dụng bởi tài khoản khác");
            }
            
            // Create new account
            Role customerRole = roleRepository.findByName("customer")
                    .orElseThrow(() -> new IllegalStateException("Không tìm thấy role 'customer'"));
            
            account = Account.builder()
                    .fullName(fullName)
                    .email(email)
                    .provider(provider)
                    .providerId(providerId)
                    .avatarUrl(avatarUrl)
                    .role(customerRole)
                    .isActive(true)
                    .build();
            
            account = accountRepository.save(account);
        }
        
        // Generate session token
        String token = UUID.randomUUID().toString();
        sessions.put(token, account.getId());
        
        return new AuthResponse(token, account.getId(), account.getFullName(),
                account.getRole() != null ? account.getRole().getName() : null,
                account.getAvatarUrl());
    }
}
