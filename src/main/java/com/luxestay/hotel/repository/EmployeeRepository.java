package com.luxestay.hotel.repository;

import com.luxestay.hotel.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Integer> {

    List<Employee> findByEmployeeCodeContainingIgnoreCase(String code);
    List<Employee> findByDepartmentContainingIgnoreCase(String department);
    List<Employee> findByPositionContainingIgnoreCase(String position);
    List<Employee> findByStatus(String status);

    @Query("""
        select e from Employee e
        where (:q is null or :q = '' or
              lower(e.employeeCode) like lower(concat('%', :q, '%')) or
              lower(e.department)   like lower(concat('%', :q, '%')) or
              lower(e.position)     like lower(concat('%', :q, '%')))
        """)
    List<Employee> search(String q);
}