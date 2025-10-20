package com.luxestay.hotel.model;


import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
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
    // Nullable 1–1 to Account
    @OneToOne(fetch = FetchType.LAZY, optional = true)   // optional=true is default; keeps it nullable at JPA level
    @JoinColumn(name = "account_id", nullable = true)    // column can be NULL
//    @JsonManagedReference
//    @JsonBackReference
    @JsonIncludeProperties({"email", "phoneNumber"})
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
    private String status = "Active";

    @Column(name = "created_at")
    private LocalDateTime createdAt;


    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}