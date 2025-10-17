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

    // Demo in-memory session tokens (token -> accountId)
    private final Map<String, Integer> sessions = new HashMap<>();

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (accountRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("Email đã tồn tại");
        }

        Role customerRole = roleRepository.findByName("account")
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

        return new AuthResponse(token, acc.getId(), acc.getFullName(), customerRole.getName());
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
                acc.getRole() != null ? acc.getRole().getName() : null);
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
}
