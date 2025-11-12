package com.luxestay.hotel.scheduler;

import com.luxestay.hotel.model.entity.BookingEntity;
import com.luxestay.hotel.repository.BookingRepository;
import com.luxestay.hotel.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class BookingNotificationScheduler {
    
    @Autowired
    private BookingRepository bookingRepository;
    
    @Autowired
    private NotificationService notificationService;
    
    /**
     * Chạy mỗi giờ để kiểm tra:
     * 1. Bookings sắp check-in (1-2 ngày trước)
     * 2. Bookings quá hạn check-in
     * 3. Payment reminders (chưa thanh toán đủ)
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour at minute 0
    public void checkBookingReminders() {
        System.out.println("[Scheduler] Running booking notification checks at: " + LocalDateTime.now());
        
        try {
            checkUpcomingCheckins();
            checkOverdueCheckins();
            checkPaymentReminders();
            cleanupOldNotifications();
        } catch (Exception e) {
            System.err.println("[Scheduler] Error in booking notification checks: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Kiểm tra bookings sắp check-in (1-2 ngày trước)
     */
    private void checkUpcomingCheckins() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDate dayAfterTomorrow = LocalDate.now().plusDays(2);
        
        // Tìm bookings confirmed và sẽ check-in trong 1-2 ngày tới
        List<BookingEntity> upcomingBookings = bookingRepository.findAll().stream()
            .filter(b -> "confirmed".equalsIgnoreCase(b.getStatus()))
            .filter(b -> b.getCheckIn() != null)
            .filter(b -> {
                LocalDate checkIn = b.getCheckIn();
                return checkIn.equals(tomorrow) || checkIn.equals(dayAfterTomorrow);
            })
            .toList();
        
        System.out.println("[Scheduler] Found " + upcomingBookings.size() + " upcoming check-ins");
        
        for (BookingEntity booking : upcomingBookings) {
            try {
                if (booking.getAccount() != null && booking.getRoom() != null) {
                    String roomName = booking.getRoom().getRoomName();
                    String checkInDate = booking.getCheckIn().toString();
                    String checkInCode = booking.getCheckInCode() != null ? booking.getCheckInCode() : "";
                    
                    notificationService.notifyUpcomingCheckin(
                        booking.getAccount().getId(),
                        booking.getId(),
                        roomName,
                        checkInDate,
                        checkInCode
                    );
                    
                    System.out.println("[Scheduler] Sent upcoming check-in notification for booking #" + booking.getId());
                }
            } catch (Exception e) {
                System.err.println("[Scheduler] Failed to send upcoming check-in notification for booking #" + booking.getId() + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Kiểm tra bookings quá hạn check-in (confirmed nhưng quá ngày check-in)
     */
    private void checkOverdueCheckins() {
        LocalDate today = LocalDate.now();
        
        // Tìm bookings confirmed nhưng đã quá ngày check-in
        List<BookingEntity> overdueBookings = bookingRepository.findAll().stream()
            .filter(b -> "confirmed".equalsIgnoreCase(b.getStatus()))
            .filter(b -> b.getCheckIn() != null)
            .filter(b -> b.getCheckIn().isBefore(today))
            .toList();
        
        System.out.println("[Scheduler] Found " + overdueBookings.size() + " overdue check-ins");
        
        for (BookingEntity booking : overdueBookings) {
            try {
                if (booking.getAccount() != null && booking.getRoom() != null) {
                    String roomName = booking.getRoom().getRoomName();
                    
                    notificationService.notifyCheckinOverdue(
                        booking.getAccount().getId(),
                        booking.getId(),
                        roomName
                    );
                    
                    System.out.println("[Scheduler] Sent overdue check-in notification for booking #" + booking.getId());
                    
                    // Optional: Auto-cancel sau X ngày quá hạn
                    // if (booking.getCheckIn().isBefore(today.minusDays(2))) {
                    //     booking.setStatus("cancelled");
                    //     booking.setCancelReason("Auto-cancelled: No-show after 2 days overdue");
                    //     bookingRepository.save(booking);
                    // }
                }
            } catch (Exception e) {
                System.err.println("[Scheduler] Failed to send overdue check-in notification for booking #" + booking.getId() + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Kiểm tra bookings cần thanh toán số tiền còn lại
     */
    private void checkPaymentReminders() {
        // Tìm bookings confirmed, đã cọc nhưng chưa thanh toán đủ
        // và sắp tới ngày check-in (trong 3-7 ngày)
        LocalDate in3Days = LocalDate.now().plusDays(3);
        LocalDate in7Days = LocalDate.now().plusDays(7);
        
        List<BookingEntity> paymentReminderBookings = bookingRepository.findAll().stream()
            .filter(b -> "confirmed".equalsIgnoreCase(b.getStatus()))
            .filter(b -> "deposit_paid".equalsIgnoreCase(b.getPaymentState()))
            .filter(b -> b.getCheckIn() != null)
            .filter(b -> {
                LocalDate checkIn = b.getCheckIn();
                return !checkIn.isBefore(in3Days) && !checkIn.isAfter(in7Days);
            })
            .toList();
        
        System.out.println("[Scheduler] Found " + paymentReminderBookings.size() + " bookings needing payment reminder");
        
        for (BookingEntity booking : paymentReminderBookings) {
            try {
                if (booking.getAccount() != null && 
                    booking.getRoom() != null && 
                    booking.getTotalPrice() != null &&
                    booking.getDepositAmount() != null) {
                    
                    Long amountRemaining = booking.getTotalPrice().longValue() - booking.getDepositAmount().longValue();
                    String roomName = booking.getRoom().getRoomName();
                    
                    if (amountRemaining > 0) {
                        notificationService.notifyPaymentReminder(
                            booking.getAccount().getId(),
                            booking.getId(),
                            amountRemaining,
                            roomName
                        );
                        
                        System.out.println("[Scheduler] Sent payment reminder for booking #" + booking.getId());
                    }
                }
            } catch (Exception e) {
                System.err.println("[Scheduler] Failed to send payment reminder for booking #" + booking.getId() + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Cleanup notifications cũ hơn 30 ngày (chạy mỗi ngày lúc 2:00 AM)
     */
    @Scheduled(cron = "0 0 2 * * *") // Every day at 2:00 AM
    public void cleanupOldNotifications() {
        try {
            System.out.println("[Scheduler] Cleaning up old notifications at: " + LocalDateTime.now());
            notificationService.cleanupOldNotifications(30);
            System.out.println("[Scheduler] Old notifications cleanup completed");
        } catch (Exception e) {
            System.err.println("[Scheduler] Error in cleanup: " + e.getMessage());
        }
    }
}

