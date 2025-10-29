package com.luxestay.hotel.repository;

import com.luxestay.hotel.model.WorkShift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WorkShiftRepository extends JpaRepository<WorkShift, Integer> {
    // Tìm tất cả ca làm việc trong một khoảng thời gian
    List<WorkShift> findByStartTimeBetween(LocalDateTime start, LocalDateTime end);

    // Tìm ca làm việc của một nhân viên cụ thể trong khoảng thời gian
    List<WorkShift> findByEmployeeIdAndStartTimeBetween(Integer employeeId, LocalDateTime start, LocalDateTime end);

    // Tìm tất cả ca làm việc của một nhân viên
    List<WorkShift> findByEmployeeId(Integer employeeId);
}
