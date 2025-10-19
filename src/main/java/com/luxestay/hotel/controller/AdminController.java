// src/main/java/com/luxestay/hotel/controller/EmployeeAdminController.java
package com.luxestay.hotel.controller;

import com.luxestay.hotel.dto.employee.EmployeeRequest;
import com.luxestay.hotel.dto.employee.EmployeeResponse;
import com.luxestay.hotel.model.Employee;
import com.luxestay.hotel.repository.AccountRepository;
import com.luxestay.hotel.repository.EmployeeRepository;
import com.luxestay.hotel.repository.RoleRepository;
import com.luxestay.hotel.service.AccountService;
import com.luxestay.hotel.service.AuthService;

import com.luxestay.hotel.model.Account;
import com.luxestay.hotel.model.Role;
import com.luxestay.hotel.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://127.0.0.1:5173",
        "http://localhost:4173",
        "http://localhost:3000"
})
@RequiredArgsConstructor
public class AdminController {


    private final AccountService accountService;
    private final EmployeeService employeeService;

    private final PasswordEncoder passwordEncoder;
    /**
     * Guard admin theo X-Auth-Token
     */
//    private void requireAdmin(HttpServletRequest request) {
//        String token = request.getHeader("X-Auth-Token");
//        if (token == null || token.isBlank()) {
//            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-Auth-Token");
//        }
//        Optional<Integer> accountIdOpt = authService.verify(token);
//        Integer accountId = accountIdOpt.orElseThrow(
//                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));
//
//        Account acc = accountRepository.findById(accountId).orElseThrow(
//                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found"));
//
//        Role role = acc.getRole();
//        String roleName = role != null ? role.getName() : null;
//        if (roleName == null || !roleName.equalsIgnoreCase("admin")) {
//            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required");
//        }
//    }

    /* ---------- CRUD EMPLOYEE ---------- */
    @GetMapping("/employees/{id}")
    public Employee get(@PathVariable("id") Integer id) {
        return employeeService.get(id);
    }

    @PostMapping("/employees")
    @ResponseStatus(HttpStatus.CREATED)
    public Employee create(@Valid @RequestBody Employee body,
                           @RequestParam(required = false) Integer accountId) {
        return employeeService.create(body, accountId);
    }

    @PutMapping("/employees/{id}")
    public Employee update(@PathVariable("id") Integer id, @RequestBody Employee patch) {
        return employeeService.update(id, patch);
    }

    @DeleteMapping("/employees/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Integer id) {
        employeeService.delete(id);
    }

    //Get all Employee
    @GetMapping("/employees")
    public List<Employee> getEmployees() {
        return employeeService.getAll();
    }


    /* ---------- CRUD ACCOUNT ---------- */
    // Get All Account
    @GetMapping("/accounts")
    public List<Account> getAccounts(HttpServletRequest request) {
//        requireAdmin(request);
        return accountService.findAll();
    }

    // GET account by ID
    @GetMapping("/accounts/{id}")
    public Account getAccount(@PathVariable("id") Integer id, HttpServletRequest request) {
        return accountService.findById(id);
    }

    // CREATE new account
    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public Account createAccount(@RequestBody Account account,
                                 @RequestParam(name = "password",required = false) String password,
                                 HttpServletRequest request) {
        if (password != null && !password.isBlank()) {
            // Ưu tiên password (plain) từ query param -> hash
            account.setPasswordHash(passwordEncoder.encode(password));
        } else {
            // Nếu không có password param, mà passwordHash đang là plain → encode (tránh lưu plain text)
            if (needsEncoding(account.getPasswordHash())) {
                account.setPasswordHash(passwordEncoder.encode(account.getPasswordHash()));
            }
        }

        accountService.saveCreate(account);
        return account;
    }

    // UPDATE account
    @PutMapping("/accounts/{id}")
    public void updateAccount(@PathVariable("id") Integer id,
                              @RequestBody Account updatedAccount,
                              @RequestParam(name = "password",required = false) String password,
                              @RequestParam(name = "active", required = false) Boolean active) {
        Account existing = accountService.findById(id);

        existing.setFullName(updatedAccount.getFullName());
        existing.setPasswordHash(updatedAccount.getPasswordHash());
        existing.setRole(updatedAccount.getRole());
        // Add other fields as needed

        // 🔐 Password
            if (password != null && !password.isBlank()) {
            existing.setPasswordHash(passwordEncoder.encode(password));
        } else if (updatedAccount.getPasswordHash() != null) {
            existing.setPasswordHash(
                    needsEncoding(updatedAccount.getPasswordHash())
                            ? passwordEncoder.encode(updatedAccount.getPasswordHash())
                            : updatedAccount.getPasswordHash()
            );
        }
        // ⚙️ Update isActive: ưu tiên body, fallback params
        if (updatedAccount.getIsActive() != null) {
            existing.setIsActive(updatedAccount.getIsActive());
        } else if (active != null) {                // 👈 fallback từ query param
            existing.setIsActive(active);
        }


        accountService.save(existing);
    }

    // DELETE account
    @DeleteMapping("/accounts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(@PathVariable("id") Integer id, HttpServletRequest request) {
        if (accountService.findById(id) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found");
        }
        accountService.delete(id);
    }
    //==========================================================================================
    private boolean needsEncoding(String rawOrHash) {
        if (rawOrHash == null || rawOrHash.isBlank()) return false; // không encode chuỗi rỗng
        String s = rawOrHash.trim();
        // Bcrypt thường bắt đầu bằng $2a$ / $2b$ / $2y$ (Spring Security BCryptPasswordEncoder)
        return !(s.startsWith("$2a$") || s.startsWith("$2b$") || s.startsWith("$2y$"));
    }
}
