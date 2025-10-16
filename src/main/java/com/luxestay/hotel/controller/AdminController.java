// src/main/java/com/luxestay/hotel/controller/EmployeeAdminController.java
package com.luxestay.hotel.controller;

import com.luxestay.hotel.dto.employee.EmployeeRequest;
import com.luxestay.hotel.dto.employee.EmployeeResponse;
import com.luxestay.hotel.repository.AccountRepository;
import com.luxestay.hotel.repository.EmployeeRepository;
import com.luxestay.hotel.service.AuthService;
import com.luxestay.hotel.service.EmployeeService;
import com.luxestay.hotel.model.Account;
import com.luxestay.hotel.model.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    private final AuthService authService;
    private final AccountRepository accountRepository;
    private final EmployeeService employeeService;

    /** Guard admin theo X-Auth-Token */
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

    /* ---------- CRUD ---------- */

    @GetMapping("/employees")
    public List<EmployeeResponse> list(@RequestParam(required = false) String q,
                                       HttpServletRequest request) {
//        requireAdmin(request);
        return employeeService.getAll(q);
    }

    @GetMapping("/{id}")
    public EmployeeResponse detail(@PathVariable Integer id, HttpServletRequest request) {
//        requireAdmin(request);
        return employeeService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeResponse create(@RequestBody EmployeeRequest req, HttpServletRequest request) {
//        requireAdmin(request);
        return employeeService.create(req);
    }

    @PutMapping("/{id}")
    public EmployeeResponse update(@PathVariable Integer id,
                                   @RequestBody EmployeeRequest req,
                                   HttpServletRequest request) {
//        requireAdmin(request);
        return employeeService.update(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id, HttpServletRequest request) {
//        requireAdmin(request);
        employeeService.delete(id);
    }

    @GetMapping("/accounts")
    public List<Account>  getAccounts(HttpServletRequest request) {
//        requireAdmin(request);
        return accountRepository.findAll();
    }
}
