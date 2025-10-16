package com.luxestay.hotel.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employees")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_id")
    private Integer id;

    // FK -> accounts.id (nullable)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @Column(name = "employee_code", length = 20)
    private String employeeCode;

    @Column(name = "position", length = 255)
    private String position;

    @Column(name = "department", length = 255)
    private String department;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    // numeric(38,2) -> BigDecimal
    @Column(name = "salary", precision = 38, scale = 2)
    private BigDecimal salary;

    // nvarchar(20)
    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "employee_gmail", length = 255)
    private String employeeGmail;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}