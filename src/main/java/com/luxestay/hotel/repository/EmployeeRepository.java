package com.luxestay.hotel.repository;

import com.luxestay.hotel.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Integer> {
}
