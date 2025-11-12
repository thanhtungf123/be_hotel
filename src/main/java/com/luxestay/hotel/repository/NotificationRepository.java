package com.luxestay.hotel.repository;

import com.luxestay.hotel.model.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
    
    // Lấy tất cả notifications của user (phân trang)
    Page<NotificationEntity> findByUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);
    
    // Lấy notifications chưa đọc của user
    List<NotificationEntity> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Integer userId);
    
    // Đếm số notifications chưa đọc
    @Query("SELECT COUNT(n) FROM NotificationEntity n WHERE n.userId = :userId AND n.isRead = false")
    Long countUnreadByUserId(@Param("userId") Integer userId);
    
    // Đánh dấu tất cả là đã đọc
    @Modifying
    @Query("UPDATE NotificationEntity n SET n.isRead = true, n.readAt = :readAt WHERE n.userId = :userId AND n.isRead = false")
    void markAllAsReadByUserId(@Param("userId") Integer userId, @Param("readAt") LocalDateTime readAt);
    
    // Xóa notifications cũ (sau 30 ngày)
    @Modifying
    @Query("DELETE FROM NotificationEntity n WHERE n.createdAt < :beforeDate")
    void deleteOlderThan(@Param("beforeDate") LocalDateTime beforeDate);
    
    // Lấy notifications theo booking ID
    List<NotificationEntity> findByBookingIdOrderByCreatedAtDesc(Integer bookingId);
    
    // Lấy notifications theo type
    List<NotificationEntity> findByUserIdAndTypeOrderByCreatedAtDesc(Integer userId, String type);
}

