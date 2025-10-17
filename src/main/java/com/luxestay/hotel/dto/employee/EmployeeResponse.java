// src/main/java/com/luxestay/hotel/dto/employee/EmployeeResponse.java
package com.luxestay.hotel.dto.employee;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @AllArgsConstructor
public class EmployeeResponse {
    private Integer id;
    private Integer accountId;
    private String employeeCode;
    private String position;
    private String department;
    private LocalDate hireDate;
    private BigDecimal salary;
    private String status;
    private LocalDateTime createdAt;

}
