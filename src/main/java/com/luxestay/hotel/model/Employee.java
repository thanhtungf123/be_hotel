package com.luxestay.hotel.model;


import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    @JsonIncludeProperties({"email", "phoneNumber", "fullName"})
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

    // Thêm mối quan hệ một-nhiều với WorkShift
    // `mappedBy = "employee"` trỏ đến trường `employee` trong class WorkShift
    // JsonIgnore để tránh lỗi lặp vô hạn khi serializing
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore // Quan trọng: Tránh vòng lặp JSON
    private List<WorkShift> workShifts = new ArrayList<>();

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}