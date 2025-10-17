// src/main/java/com/luxestay/hotel/dto/employee/EmployeeRequest.java
package com.luxestay.hotel.dto.employee;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class EmployeeRequest {
    private Integer accountId;     // có thể null
    private String employeeCode;
    private String position;
    private String department;
    private LocalDate hireDate;
    private BigDecimal salary;
    private String status;

}
