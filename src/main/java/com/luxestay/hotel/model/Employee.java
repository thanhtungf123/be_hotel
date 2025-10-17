package com.luxestay.hotel.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String employeeGmail;
    private String position;
    private String department;
    private LocalDate hireDate;
    private BigDecimal salary;
    
    @Transient
    private String password; // Transient field for frontend compatibility
    private int status = 1; // 1: active, 0: inactive
    //======================================================


    //======================================================


//    public int getId() {
//        return id;
//    }
//
//    public void setId(int id) {
//        this.id = id;
//    }
//
//
//
//    public String getPosition() {
//        return position;
//    }
//
//    public void setPosition(String position) {
//        this.position = position;
//    }
//
//    public String getDepartment() {
//        return department;
//    }
//
//    public void setDepartment(String department) {
//        this.department = department;
//    }
//
//    public LocalDate getHireDate() {
//        return hireDate;
//    }
//
//    public void setHireDate(LocalDate hireDate) {
//        this.hireDate = hireDate;
//    }
//
//    public BigDecimal getSalary() {
//        return salary;
//    }
//
//    public void setSalary(BigDecimal salary) {
//        this.salary = salary;
//    }
//
//    public int getStatus() {
//        return status;
//    }
//
//    public void setStatus(int status) {
//        this.status = status;
//    }
//
//
//    public String getPassword() {
//        return password;
//    }
//
//    public void setPassword(String password) {
//        this.password = password;
//    }
}
