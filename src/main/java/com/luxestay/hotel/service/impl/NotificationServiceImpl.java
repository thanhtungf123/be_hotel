package com.luxestay.hotel.service.impl;

import com.luxestay.hotel.model.entity.NotificationEntity;
import com.luxestay.hotel.repository.NotificationRepository;
import com.luxestay.hotel.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {
    
    @Autowired
    private NotificationRepository notificationRepository;
    
    @Override
    public Page<NotificationEntity> getUserNotifications(Integer userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
    
    @Override
    public List<NotificationEntity> getUnreadNotifications(Integer userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }
    
    @Override
    public Long countUnread(Integer userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }
    
    @Override
    @Transactional
    public void markAsRead(Long notificationId, Integer userId) {
        NotificationEntity notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new RuntimeException("Notification not found"));
        
        if (!notification.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        if (!notification.getIsRead()) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }
    
    @Override
    @Transactional
    public void markAllAsRead(Integer userId) {
        notificationRepository.markAllAsReadByUserId(userId, LocalDateTime.now());
    }
    
    @Override
    @Transactional
    public void deleteNotification(Long notificationId, Integer userId) {
        NotificationEntity notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new RuntimeException("Notification not found"));
        
        if (!notification.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        notificationRepository.delete(notification);
    }
    
    @Override
    @Transactional
    public void cleanupOldNotifications(int daysOld) {
        LocalDateTime beforeDate = LocalDateTime.now().minusDays(daysOld);
        notificationRepository.deleteOlderThan(beforeDate);
    }
    
    // === Notification creation methods ===
    
    @Override
    @Transactional
    public void notifyBookingSuccess(Integer userId, Integer bookingId, String roomName, String checkIn, String checkOut) {
        NotificationEntity notification = new NotificationEntity(
            userId,
            "BOOKING_SUCCESS",
            "Đặt phòng thành công!",
            String.format("Bạn đã đặt phòng %s thành công từ %s đến %s. Vui lòng thanh toán để xác nhận đặt phòng.", 
                roomName, checkIn, checkOut)
        );
        notification.setBookingId(bookingId);
        notification.setActionUrl("/account/bookings");
        notification.setPriority("HIGH");
        notificationRepository.save(notification);
    }
    
    @Override
    @Transactional
    public void notifyBookingConfirmed(Integer userId, Integer bookingId, String checkInCode) {
        NotificationEntity notification = new NotificationEntity(
            userId,
            "BOOKING_CONFIRMED",
            "Đặt phòng đã được xác nhận!",
            String.format("Đặt phòng của bạn đã được xác nhận. Mã check-in của bạn là: %s. Vui lòng giữ mã này để check-in.", 
                checkInCode)
        );
        notification.setBookingId(bookingId);
        notification.setActionUrl("/account/bookings");
        notification.setPriority("HIGH");
        notificationRepository.save(notification);
    }
    
    @Override
    @Transactional
    public void notifyPaymentReminder(Integer userId, Integer bookingId, Long amountRemaining, String roomName) {
        NotificationEntity notification = new NotificationEntity(
            userId,
            "PAYMENT_REMINDER",
            "Nhắc nhở thanh toán",
            String.format("Bạn còn %s chưa thanh toán cho phòng %s. Vui lòng hoàn tất thanh toán để tránh bị hủy đặt phòng.", 
                formatVND(amountRemaining), roomName)
        );
        notification.setBookingId(bookingId);
        notification.setActionUrl("/account/bookings");
        notification.setPriority("HIGH");
        notificationRepository.save(notification);
    }
    
    @Override
    @Transactional
    public void notifyUpcomingCheckin(Integer userId, Integer bookingId, String roomName, String checkInDate, String checkInCode) {
        NotificationEntity notification = new NotificationEntity(
            userId,
            "CHECKIN_REMINDER",
            "Sắp tới ngày nhận phòng",
            String.format("Bạn sẽ nhận phòng %s vào ngày %s. Mã check-in: %s. Thời gian check-in: 14:00.", 
                roomName, checkInDate, checkInCode)
        );
        notification.setBookingId(bookingId);
        notification.setActionUrl("/account/bookings");
        notification.setPriority("NORMAL");
        notificationRepository.save(notification);
    }
    
    @Override
    @Transactional
    public void notifyCheckinOverdue(Integer userId, Integer bookingId, String roomName) {
        NotificationEntity notification = new NotificationEntity(
            userId,
            "CHECKIN_OVERDUE",
            "Quá hạn check-in",
            String.format("Bạn đã quá hạn check-in cho phòng %s. Vui lòng liên hệ khách sạn hoặc đặt phòng của bạn có thể bị hủy.", 
                roomName)
        );
        notification.setBookingId(bookingId);
        notification.setActionUrl("/account/bookings");
        notification.setPriority("URGENT");
        notificationRepository.save(notification);
    }
    
    @Override
    @Transactional
    public void notifyCheckinSuccess(Integer userId, Integer bookingId, String roomName) {
        NotificationEntity notification = new NotificationEntity(
            userId,
            "CHECKIN_SUCCESS",
            "Check-in thành công!",
            String.format("Bạn đã check-in phòng %s thành công. Chúc bạn có kỳ nghỉ vui vẻ!", roomName)
        );
        notification.setBookingId(bookingId);
        notification.setActionUrl("/account/bookings");
        notification.setPriority("NORMAL");
        notificationRepository.save(notification);
    }
    
    @Override
    @Transactional
    public void notifyCheckoutSuccess(Integer userId, Integer bookingId, String roomName) {
        NotificationEntity notification = new NotificationEntity(
            userId,
            "CHECKOUT_SUCCESS",
            "Check-out thành công!",
            String.format("Bạn đã check-out khỏi phòng %s. Cảm ơn bạn đã sử dụng dịch vụ của chúng tôi. Hẹn gặp lại!", 
                roomName)
        );
        notification.setBookingId(bookingId);
        notification.setActionUrl("/account/bookings");
        notification.setPriority("NORMAL");
        notificationRepository.save(notification);
    }
    
    @Override
    @Transactional
    public void notifyCancellationApproved(Integer userId, Integer bookingId, String roomName) {
        NotificationEntity notification = new NotificationEntity(
            userId,
            "CANCELLATION_APPROVED",
            "Yêu cầu hủy phòng đã được duyệt",
            String.format("Yêu cầu hủy phòng %s của bạn đã được chấp nhận. Chúng tôi sẽ xử lý hoàn tiền trong thời gian sớm nhất.", 
                roomName)
        );
        notification.setBookingId(bookingId);
        notification.setActionUrl("/account/bookings");
        notification.setPriority("HIGH");
        notificationRepository.save(notification);
    }
    
    @Override
    @Transactional
    public void notifyCancellationRejected(Integer userId, Integer bookingId, String roomName, String reason) {
        NotificationEntity notification = new NotificationEntity(
            userId,
            "CANCELLATION_REJECTED",
            "Yêu cầu hủy phòng bị từ chối",
            String.format("Yêu cầu hủy phòng %s của bạn đã bị từ chối. Lý do: %s", roomName, reason)
        );
        notification.setBookingId(bookingId);
        notification.setActionUrl("/account/bookings");
        notification.setPriority("HIGH");
        notificationRepository.save(notification);
    }
    
    @Override
    @Transactional
    public void notifyRefundProcessing(Integer userId, Integer bookingId, Long refundAmount) {
        NotificationEntity notification = new NotificationEntity(
            userId,
            "REFUND_PROCESSING",
            "Đang xử lý hoàn tiền",
            String.format("Chúng tôi đang xử lý hoàn tiền %s cho bạn. Tiền sẽ được chuyển về tài khoản trong 3-5 ngày làm việc.", 
                formatVND(refundAmount))
        );
        notification.setBookingId(bookingId);
        notification.setActionUrl("/account/bookings");
        notification.setPriority("NORMAL");
        notificationRepository.save(notification);
    }
    
    @Override
    @Transactional
    public void notifyRefundCompleted(Integer userId, Integer bookingId, Long refundAmount) {
        NotificationEntity notification = new NotificationEntity(
            userId,
            "REFUND_COMPLETED",
            "Hoàn tiền thành công!",
            String.format("Chúng tôi đã hoàn %s về tài khoản của bạn. Vui lòng kiểm tra.", 
                formatVND(refundAmount))
        );
        notification.setBookingId(bookingId);
        notification.setActionUrl("/account/bookings");
        notification.setPriority("HIGH");
        notificationRepository.save(notification);
    }
    
    // Helper method
    private String formatVND(Long amount) {
        return String.format("%,d₫", amount);
    }
}

