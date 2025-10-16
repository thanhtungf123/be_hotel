package com.luxestay.hotel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EmployeeListItem {
    private Integer id;
    private String name;
    private String email;
    private String phone;
    private String position;
    private String department;
    private String status;
}
