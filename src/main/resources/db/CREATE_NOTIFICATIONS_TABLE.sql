-- ============================================
-- NOTIFICATION SYSTEM - DATABASE SCHEMA
-- ============================================

-- Tạo bảng notifications
CREATE TABLE notifications (
    notification_id BIGINT PRIMARY KEY IDENTITY(1,1),
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    booking_id BIGINT NULL,
    is_read BIT NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    read_at DATETIME2 NULL,
    action_url VARCHAR(500) NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    
    -- Foreign key constraints
    CONSTRAINT FK_notifications_user FOREIGN KEY (user_id) REFERENCES accounts(account_id) ON DELETE CASCADE,
    CONSTRAINT FK_notifications_booking FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE SET NULL
);

-- Indexes để tăng performance
CREATE INDEX IX_notifications_user_id ON notifications(user_id);
CREATE INDEX IX_notifications_created_at ON notifications(created_at DESC);
CREATE INDEX IX_notifications_is_read ON notifications(is_read) WHERE is_read = 0;
CREATE INDEX IX_notifications_type ON notifications(type);
CREATE INDEX IX_notifications_booking_id ON notifications(booking_id);

-- Index composite cho query phổ biến nhất: user + unread + created_at
CREATE INDEX IX_notifications_user_unread_created ON notifications(user_id, is_read, created_at DESC);

-- ============================================
-- SAMPLE DATA (for testing)
-- ============================================

-- Uncomment to insert sample notifications
/*
INSERT INTO notifications (user_id, type, title, message, booking_id, priority, action_url) VALUES
(1, 'BOOKING_SUCCESS', '✅ Đặt phòng thành công!', 'Bạn đã đặt phòng Deluxe Suite thành công từ 2025-11-15 đến 2025-11-20.', 1, 'HIGH', '/account/bookings'),
(1, 'PAYMENT_REMINDER', '💳 Nhắc nhở thanh toán', 'Bạn còn 5,000,000₫ chưa thanh toán cho phòng Deluxe Suite.', 1, 'HIGH', '/account/bookings'),
(1, 'CHECKIN_REMINDER', '🔔 Sắp tới ngày nhận phòng', 'Bạn sẽ nhận phòng Deluxe Suite vào ngày 2025-11-15. Mã check-in: ABC123', 1, 'NORMAL', '/account/bookings');
*/

-- ============================================
-- CLEANUP OLD NOTIFICATIONS (Run periodically)
-- ============================================

-- Delete notifications older than 30 days (will be handled by scheduler)
-- DELETE FROM notifications WHERE created_at < DATEADD(day, -30, GETDATE());

