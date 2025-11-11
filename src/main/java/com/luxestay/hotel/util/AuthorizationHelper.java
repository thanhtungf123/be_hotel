package com.luxestay.hotel.util;

import com.luxestay.hotel.model.Account;
import com.luxestay.hotel.model.Role;
import com.luxestay.hotel.repository.AccountRepository;
import com.luxestay.hotel.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

/**
 * Helper class for authorization checks
 */
@Component
@RequiredArgsConstructor
public class AuthorizationHelper {

    private final AuthService authService;
    private final AccountRepository accountRepository;

    /**
     * Require admin role for the current request
     * Throws ResponseStatusException if not authorized
     */
    public Account requireAdmin(HttpServletRequest request) {
        String token = request.getHeader("X-Auth-Token");
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-Auth-Token header");
        }

        Optional<Integer> accountIdOpt = authService.verify(token);
        Integer accountId = accountIdOpt.orElseThrow(
                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token"));

        Account acc = accountRepository.findById(accountId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found"));

        Role role = acc.getRole();
        String roleName = role != null ? role.getName() : null;

        if (roleName == null || !roleName.equalsIgnoreCase("admin")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required");
        }

        return acc;
    }

    /**
     * Require admin or manager role
     */
    public Account requireAdminOrManager(HttpServletRequest request) {
        String token = request.getHeader("X-Auth-Token");
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-Auth-Token header");
        }

        Optional<Integer> accountIdOpt = authService.verify(token);
        Integer accountId = accountIdOpt.orElseThrow(
                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token"));

        Account acc = accountRepository.findById(accountId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found"));

        Role role = acc.getRole();
        String roleName = role != null ? role.getName() : null;

        if (roleName == null ||
                (!roleName.equalsIgnoreCase("admin") && !roleName.equalsIgnoreCase("manager"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin or Manager role required");
        }

        return acc;
    }

    /**
     * Require admin or staff role
     * Allows both admin and staff to access
     */
    public Account requireAdminOrStaff(HttpServletRequest request) {
        String token = request.getHeader("X-Auth-Token");
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-Auth-Token header");
        }

        Optional<Integer> accountIdOpt = authService.verify(token);
        Integer accountId = accountIdOpt.orElseThrow(
                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token"));

        Account acc = accountRepository.findById(accountId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found"));

        Role role = acc.getRole();
        String roleName = role != null ? role.getName() : null;

        if (roleName == null ||
                (!roleName.equalsIgnoreCase("admin") && !roleName.equalsIgnoreCase("staff"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin or Staff role required");
        }

        return acc;
    }

    /**
     * Get current authenticated account (if any)
     */
    public Optional<Account> getCurrentAccount(HttpServletRequest request) {
        String token = request.getHeader("X-Auth-Token");
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        Optional<Integer> accountIdOpt = authService.verify(token);
        if (accountIdOpt.isEmpty()) {
            return Optional.empty();
        }

        return accountRepository.findById(accountIdOpt.get());
    }
}


