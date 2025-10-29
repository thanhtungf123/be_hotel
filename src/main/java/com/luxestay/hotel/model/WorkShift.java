package com.luxestay.hotel.model;

import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "work_shifts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkShift {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    // Mối quan hệ nhiều-một với Employee
    // Nhiều ca làm việc có thể thuộc về một nhân viên
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    @JsonIncludeProperties({"id", "employeeCode", "account"})// Chỉ bao gồm các trường này khi serialize Employee để tránh lặp
    private Employee employee;

    private String shiftDetails;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;





}
