-- ============================================
-- NOTIFICATION SYSTEM - DATABASE SCHEMA (SAFE VERSION)
-- Xóa bảng cũ nếu tồn tại và tạo lại
-- ============================================

-- Xóa indexes nếu tồn tại
IF EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_notifications_user_unread_created' AND object_id = OBJECT_ID('notifications'))
    DROP INDEX IX_notifications_user_unread_created ON notifications;

IF EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_notifications_booking_id' AND object_id = OBJECT_ID('notifications'))
    DROP INDEX IX_notifications_booking_id ON notifications;

IF EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_notifications_type' AND object_id = OBJECT_ID('notifications'))
    DROP INDEX IX_notifications_type ON notifications;

IF EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_notifications_is_read' AND object_id = OBJECT_ID('notifications'))
    DROP INDEX IX_notifications_is_read ON notifications;

IF EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_notifications_created_at' AND object_id = OBJECT_ID('notifications'))
    DROP INDEX IX_notifications_created_at ON notifications;

IF EXISTS (SELECT * FROM sys.indexes WHERE name = 'IX_notifications_user_id' AND object_id = OBJECT_ID('notifications'))
    DROP INDEX IX_notifications_user_id ON notifications;

-- Xóa foreign key constraints nếu tồn tại
IF EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_notifications_booking')
    ALTER TABLE notifications DROP CONSTRAINT FK_notifications_booking;

IF EXISTS (SELECT * FROM sys.foreign_keys WHERE name = 'FK_notifications_user')
    ALTER TABLE notifications DROP CONSTRAINT FK_notifications_user;

-- Xóa bảng nếu tồn tại
IF OBJECT_ID('notifications', 'U') IS NOT NULL
    DROP TABLE notifications;

GO

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

GO

-- Indexes để tăng performance
CREATE INDEX IX_notifications_user_id ON notifications(user_id);
CREATE INDEX IX_notifications_created_at ON notifications(created_at DESC);
CREATE INDEX IX_notifications_is_read ON notifications(is_read) WHERE is_read = 0;
CREATE INDEX IX_notifications_type ON notifications(type);
CREATE INDEX IX_notifications_booking_id ON notifications(booking_id);

-- Index composite cho query phổ biến nhất: user + unread + created_at
CREATE INDEX IX_notifications_user_unread_created ON notifications(user_id, is_read, created_at DESC);

GO

-- ============================================
-- SAMPLE DATA (for testing)
-- ============================================

-- Insert test notifications cho user có account_id = 1
-- Uncomment phần này nếu muốn test
/*
INSERT INTO notifications (user_id, type, title, message, booking_id, priority, action_url) VALUES
(1, 'BOOKING_SUCCESS', '✅ Đặt phòng thành công!', 'Bạn đã đặt phòng Deluxe Suite thành công từ 2025-11-15 đến 2025-11-20.', NULL, 'HIGH', '/account/bookings'),
(1, 'PAYMENT_REMINDER', '💳 Nhắc nhở thanh toán', 'Bạn còn 5,000,000₫ chưa thanh toán cho phòng Deluxe Suite. Vui lòng hoàn tất thanh toán.', NULL, 'HIGH', '/account/bookings'),
(1, 'CHECKIN_REMINDER', '🔔 Sắp tới ngày nhận phòng', 'Bạn sẽ nhận phòng Deluxe Suite vào ngày 2025-11-15. Mã check-in: ABC123. Thời gian check-in: 14:00.', NULL, 'NORMAL', '/account/bookings');
*/

GO

-- Verify
SELECT COUNT(*) AS TotalNotifications FROM notifications;
SELECT 'Notifications table created successfully!' AS Result;

GO

