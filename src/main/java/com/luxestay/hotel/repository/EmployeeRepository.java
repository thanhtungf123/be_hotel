package com.luxestay.hotel.repository;

import com.luxestay.hotel.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Integer> {
    Optional<Employee> findByAccount_Id(Integer accountId);
    boolean existsByAccount_Id(Integer accountId);
    Optional<Employee> findById(Integer id);
}