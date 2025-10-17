package com.luxestay.hotel.service;

import com.luxestay.hotel.repository.AccountRepository;
import com.luxestay.hotel.repository.EmployeeRepository;
import com.luxestay.hotel.repository.RoleRepository;
import com.luxestay.hotel.model.Account;
import com.luxestay.hotel.model.Employee;
import com.luxestay.hotel.model.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeService {
    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private RoleRepository roleRepository;

    public List<Employee> getAllEmployees(){
        return employeeRepository.findAll();
    }

    public Employee getEmployeeById(int id){
        return employeeRepository.findById(id).get();
    }

    public void addEmployee(Employee employee){
        try{
            employeeRepository.save(employee);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create employee: " + e.getMessage(), e);
        }
    }

    public void updateEmployee(Employee employee) {
        Employee existingEmployee = employeeRepository.findById(employee.getId()).orElseThrow();
        
        // Update basic fields
        if (employee.getEmployeeGmail() != null) {
            existingEmployee.setEmployeeGmail(employee.getEmployeeGmail());
        }
        if (employee.getPosition() != null) {
            existingEmployee.setPosition(employee.getPosition());
        }
        if (employee.getDepartment() != null) {
            existingEmployee.setDepartment(employee.getDepartment());
        }
        if (employee.getSalary() != null) {
            existingEmployee.setSalary(employee.getSalary());
        }
        if (employee.getStatus() != existingEmployee.getStatus()) {
            existingEmployee.setStatus(employee.getStatus());
        }
        
//        // Update password if provided
//        if (employee.getPassword() != null && !employee.getPassword().trim().isEmpty()) {
//            Account account = existingEmployee.getAccount();
//            if (account != null) {
//                account.setPasswordHash(employee.getPassword());
//                accountRepository.save(account);
//            }
//        }
        
        // Save the updated employee
        employeeRepository.save(existingEmployee);
    }

    public void deleteEmployee(int id){
        // Soft delete: set status to 0 instead of removing from database
        Employee employee = employeeRepository.findById(id).orElse(null);
        if (employee != null) {
            employee.setStatus(0);
            employeeRepository.save(employee);
        }
    }
}
