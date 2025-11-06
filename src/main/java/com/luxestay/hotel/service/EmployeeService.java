package com.luxestay.hotel.service;

import com.luxestay.hotel.model.Account;
import com.luxestay.hotel.model.Employee;
import com.luxestay.hotel.model.entity.BookingEntity;
import com.luxestay.hotel.repository.AccountRepository;
import com.luxestay.hotel.repository.BookingRepository;
import com.luxestay.hotel.repository.EmployeeRepository;
import com.luxestay.hotel.repository.RoleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeService {

    private final EmployeeRepository employeeRepo;
    private final AccountRepository accountRepo;
    private final BookingRepository bookingRepo;
    private final RoleRepository roleRepo;

    public Employee create(Employee e, Integer accountIdOrNull) {
        if (accountIdOrNull != null) {
            Account acc = accountRepo.findById(accountIdOrNull)
                    .orElseThrow(() -> new NoSuchElementException("Account not found: " + accountIdOrNull));
            ensureAccountNotLinked(acc.getId(), null);
            acc.setRole(roleRepo.findById(2).orElse(null));
            e.setAccount(acc);
        }
        return employeeRepo.save(e);
    }

    public Employee get(Integer id) {
        return employeeRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Employee not found: " + id));
    }

    public Page<Employee> list(Pageable pageable) {
        return employeeRepo.findAll(pageable);
    }

    public Employee update(Integer id, Employee patch) {
        Employee e = employeeRepo.findById(id).orElseThrow(() -> new NoSuchElementException("Employee not found: " + id));
        if (patch.getEmployeeCode() != null) e.setEmployeeCode(patch.getEmployeeCode());
        if (patch.getPosition() != null) e.setPosition(patch.getPosition());
        if (patch.getDepartment() != null) e.setDepartment(patch.getDepartment());
        if (patch.getHireDate() != null) e.setHireDate(patch.getHireDate());
        if (patch.getSalary() != null) e.setSalary(patch.getSalary());
        if (patch.getStatus() != null) e.setStatus(patch.getStatus());
        return employeeRepo.save(e);
    }


    public void delete(Integer id) {
        Employee e = employeeRepo.findById(id).orElseThrow(() -> new NoSuchElementException("Employee not found: " + id));
        if(e != null) {
            e.setStatus("terminated");
            employeeRepo.save(e);
        }
    }


    public Employee linkAccount(Integer employeeId, Integer accountId) {
        Employee e = get(employeeId);
        Account acc = accountRepo.findById(accountId)
                .orElseThrow(() -> new NoSuchElementException("Account not found: " + accountId));
        ensureAccountNotLinked(acc.getId(), e.getId());
        e.setAccount(acc);
        return e;
    }


    public Employee unlinkAccount(Integer employeeId) {
        Employee e = get(employeeId);
        e.setAccount(null);
        return e;
    }

    private void ensureAccountNotLinked(Integer accountId, Integer currentEmployeeIdOrNull) {
        employeeRepo.findByAccount_Id(accountId).ifPresent(existing -> {
            if (currentEmployeeIdOrNull == null || !existing.getId().equals(currentEmployeeIdOrNull)) {
                throw new IllegalStateException("Account " + accountId + " is already linked to employee " + existing.getId());
            }
        });
    }

    public List<Employee> getAll() {
        return employeeRepo.findAll();
    }

    public List<BookingEntity> getHistory(Integer id) {
        return bookingRepo.findAllByAccount_Id(id).stream().toList();
    }
}