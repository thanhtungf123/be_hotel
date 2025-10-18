package com.luxestay.hotel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AccountListItem {
    private Integer id;
    private String fullName;
    private String username;
    private String email;
    private String phone;
    /** role dạng string: "admin" | "employee" | "account" */
    private String role;
    /** status: "active" | "disabled" ... */
    private String status;
}
