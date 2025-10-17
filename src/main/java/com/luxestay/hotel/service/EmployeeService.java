// src/main/java/com/luxestay/hotel/service/EmployeeService.java
package com.luxestay.hotel.service;

import com.luxestay.hotel.dto.employee.EmployeeRequest;
import com.luxestay.hotel.dto.employee.EmployeeResponse;
import com.luxestay.hotel.model.Account;
import com.luxestay.hotel.model.Employee;
import com.luxestay.hotel.repository.AccountRepository;
import com.luxestay.hotel.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.http.HttpStatus;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final AccountRepository accountRepository;

    public List<EmployeeResponse> getAll(String q) {
        List<Employee> list = (q == null || q.isBlank())
                ? employeeRepository.findAll()
                : employeeRepository.search(q);

        return list.stream().map(this::toResp).toList();
    }

    public EmployeeResponse getById(Integer id) {
        Employee e = employeeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
        return toResp(e);
    }

    @Transactional
    public EmployeeResponse create(EmployeeRequest r) {
        Employee e = new Employee();
        apply(e, r);
        e = employeeRepository.save(e);
        return toResp(e);
    }

    @Transactional
    public EmployeeResponse update(Integer id, EmployeeRequest r) {
        Employee e = employeeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
        apply(e, r);
        e = employeeRepository.save(e);
        return toResp(e);
    }

    @Transactional
    public void delete(Integer id) {
        Employee e = employeeRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
        if(e != null) {
            e.setStatus("deactive");
            employeeRepository.save(e);
        }
    }

    /* ---------- helpers ---------- */

    private void apply(Employee e, EmployeeRequest r) {
        Account acc = null;
        if (r.getAccountId() != null) {
            acc = accountRepository.findById(r.getAccountId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account not found"));
        }
        e.setAccount(acc);
        e.setEmployeeCode(r.getEmployeeCode());
        e.setPosition(r.getPosition());
        e.setDepartment(r.getDepartment());
        e.setHireDate(r.getHireDate());
        e.setSalary(r.getSalary());
        e.setStatus(r.getStatus());

        // created_at tự set ở @PrePersist
    }

    private EmployeeResponse toResp(Employee e) {
        return new EmployeeResponse(
                e.getId(),
                e.getAccount() != null ? e.getAccount().getId() : null,
                e.getEmployeeCode(),
                e.getPosition(),
                e.getDepartment(),
                e.getHireDate(),
                e.getSalary(),
                e.getStatus(),
                e.getCreatedAt()
        );
    }
}
