package com.luxestay.hotel.service;

import com.luxestay.hotel.model.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationService {
    
    // Lấy tất cả notifications của user (phân trang)
    Page<NotificationEntity> getUserNotifications(Integer userId, Pageable pageable);
    
    // Lấy notifications chưa đọc
    List<NotificationEntity> getUnreadNotifications(Integer userId);
    
    // Đếm số notifications chưa đọc
    Long countUnread(Integer userId);
    
    // Đánh dấu một notification là đã đọc
    void markAsRead(Long notificationId, Integer userId);
    
    // Đánh dấu tất cả là đã đọc
    void markAllAsRead(Integer userId);
    
    // Xóa notification
    void deleteNotification(Long notificationId, Integer userId);
    
    // Xóa notifications cũ (cleanup task)
    void cleanupOldNotifications(int daysOld);
    
    // === Các method tạo notification cho các sự kiện khác nhau ===
    
    // Notification khi booking thành công
    void notifyBookingSuccess(Integer userId, Integer bookingId, String roomName, String checkIn, String checkOut);
    
    // Notification khi booking được xác nhận
    void notifyBookingConfirmed(Integer userId, Integer bookingId, String checkInCode);
    
    // Notification yêu cầu thanh toán số tiền còn lại
    void notifyPaymentReminder(Integer userId, Integer bookingId, Long amountRemaining, String roomName);
    
    // Notification sắp tới ngày check-in (1-2 ngày trước)
    void notifyUpcomingCheckin(Integer userId, Integer bookingId, String roomName, String checkInDate, String checkInCode);
    
    // Notification quá hạn check-in
    void notifyCheckinOverdue(Integer userId, Integer bookingId, String roomName);
    
    // Notification check-in thành công
    void notifyCheckinSuccess(Integer userId, Integer bookingId, String roomName);
    
    // Notification check-out thành công
    void notifyCheckoutSuccess(Integer userId, Integer bookingId, String roomName);
    
    // Notification yêu cầu hủy booking được duyệt
    void notifyCancellationApproved(Integer userId, Integer bookingId, String roomName);
    
    // Notification yêu cầu hủy booking bị từ chối
    void notifyCancellationRejected(Integer userId, Integer bookingId, String roomName, String reason);
    
    // Notification về hoàn tiền
    void notifyRefundProcessing(Integer userId, Integer bookingId, Long refundAmount);
    
    void notifyRefundCompleted(Integer userId, Integer bookingId, Long refundAmount);
}

