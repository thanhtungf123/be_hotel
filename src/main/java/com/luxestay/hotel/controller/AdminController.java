// src/main/java/com/luxestay/hotel/controller/EmployeeAdminController.java
package com.luxestay.hotel.controller;

import com.luxestay.hotel.dto.employee.EmployeeRequest;
import com.luxestay.hotel.dto.employee.EmployeeResponse;
import com.luxestay.hotel.repository.AccountRepository;
import com.luxestay.hotel.repository.EmployeeRepository;
import com.luxestay.hotel.service.AccountService;
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
//    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final EmployeeService employeeService;

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
    public EmployeeResponse detail(@PathVariable Integer id, HttpServletRequest request) {
        return employeeService.getById(id);
    }

    @PostMapping("/employees")
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeResponse create(@RequestBody EmployeeRequest req, HttpServletRequest request) {
        return employeeService.create(req);
    }

    @PutMapping("/employees/{id}/edit")
    public EmployeeResponse update(@PathVariable Integer id,
                                   @RequestBody EmployeeRequest req,
                                   HttpServletRequest request) {
        return employeeService.update(id, req);
    }

    @DeleteMapping("/employees/{id}/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Integer id, HttpServletRequest request) {
        employeeService.delete(id);
    }

    //Get all Employee
    @GetMapping("/employees/employees")
    public List<EmployeeResponse> list(@RequestParam(required = false) String q,
                                       HttpServletRequest request) {
//
        return employeeService.getAll(q);
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
    public Account getAccount(@PathVariable Integer id, HttpServletRequest request) {
        return accountService.findById(id);
    }

    // CREATE new account
    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public void createAccount(@RequestBody Account account, HttpServletRequest request) {
        accountService.save(account);
    }

    // UPDATE account
    @PutMapping("/accounts/{id}/edit")
    public void updateAccount(@PathVariable Integer id,
                                 @RequestBody Account updatedAccount,
                                 HttpServletRequest request) {
        Account existing = accountService.findById(id);

        existing.setFullName(updatedAccount.getFullName());
        existing.setPasswordHash(updatedAccount.getPasswordHash());
        existing.setRole(updatedAccount.getRole());
        // Add other fields as needed

        accountService.save(existing);
    }

    // DELETE account
    @DeleteMapping("/accounts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(@PathVariable Integer id, HttpServletRequest request) {
        if (accountService.findById(id) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found");
        }
        accountService.delete(id);
    }

}
