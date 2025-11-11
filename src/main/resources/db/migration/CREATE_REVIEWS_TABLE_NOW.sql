-- ============================================
-- QUICK SETUP: Tạo bảng reviews ngay lập tức
-- ============================================
-- Copy toàn bộ nội dung này và chạy trong SQL Server Management Studio
-- Database: hotel_booking_system

USE [hotel_booking_system]
GO

-- Xóa bảng nếu đã tồn tại (CẨN THẬN: Sẽ mất dữ liệu)
-- DROP TABLE IF EXISTS [dbo].[reviews];
-- GO

-- Tạo bảng reviews
IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.TABLES 
    WHERE TABLE_SCHEMA = 'dbo' 
    AND TABLE_NAME = 'reviews'
)
BEGIN
    CREATE TABLE [dbo].[reviews](
        [review_id] [int] IDENTITY(1,1) NOT NULL,
        [booking_id] [int] NOT NULL,
        [rating] [int] NOT NULL,
        [comment] [nvarchar](max) NULL,
        [created_at] [datetime] NOT NULL DEFAULT GETDATE(),
        CONSTRAINT [PK_reviews] PRIMARY KEY CLUSTERED ([review_id] ASC)
    );
    PRINT '✅ Đã tạo bảng reviews thành công!';
END
ELSE
BEGIN
    PRINT '⚠️ Bảng reviews đã tồn tại.';
END
GO

-- Tạo Foreign Key
IF NOT EXISTS (
    SELECT 1 FROM sys.foreign_keys 
    WHERE name = 'FK_reviews_booking' 
    AND parent_object_id = OBJECT_ID('dbo.reviews')
)
BEGIN
    ALTER TABLE [dbo].[reviews]
    ADD CONSTRAINT [FK_reviews_booking] 
        FOREIGN KEY ([booking_id]) 
        REFERENCES [dbo].[bookings]([booking_id]) 
        ON DELETE CASCADE;
    PRINT '✅ Đã tạo Foreign Key FK_reviews_booking';
END
ELSE
BEGIN
    PRINT '⚠️ Foreign Key FK_reviews_booking đã tồn tại.';
END
GO

-- Tạo Check Constraint cho rating
IF NOT EXISTS (
    SELECT 1 FROM sys.check_constraints 
    WHERE name = 'CK_reviews_rating' 
    AND parent_object_id = OBJECT_ID('dbo.reviews')
)
BEGIN
    ALTER TABLE [dbo].[reviews]
    ADD CONSTRAINT [CK_reviews_rating] 
        CHECK ([rating] >= 1 AND [rating] <= 5);
    PRINT '✅ Đã tạo Check Constraint CK_reviews_rating';
END
ELSE
BEGIN
    PRINT '⚠️ Check Constraint CK_reviews_rating đã tồn tại.';
END
GO

-- Tạo Index cho booking_id
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes 
    WHERE name = 'IX_reviews_booking_id' 
    AND object_id = OBJECT_ID('dbo.reviews')
)
BEGIN
    CREATE NONCLUSTERED INDEX [IX_reviews_booking_id] 
    ON [dbo].[reviews]([booking_id]);
    PRINT '✅ Đã tạo Index IX_reviews_booking_id';
END
ELSE
BEGIN
    PRINT '⚠️ Index IX_reviews_booking_id đã tồn tại.';
END
GO

-- Tạo Index cho created_at
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes 
    WHERE name = 'IX_reviews_created_at' 
    AND object_id = OBJECT_ID('dbo.reviews')
)
BEGIN
    CREATE NONCLUSTERED INDEX [IX_reviews_created_at] 
    ON [dbo].[reviews]([created_at] DESC);
    PRINT '✅ Đã tạo Index IX_reviews_created_at';
END
ELSE
BEGIN
    PRINT '⚠️ Index IX_reviews_created_at đã tồn tại.';
END
GO

-- Kiểm tra kết quả
SELECT 
    'Bảng reviews' AS Item,
    CASE WHEN EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.TABLES 
        WHERE TABLE_NAME = 'reviews'
    ) THEN '✅ Đã tạo' ELSE '❌ Chưa tạo' END AS Status
UNION ALL
SELECT 
    'Foreign Key FK_reviews_booking',
    CASE WHEN EXISTS (
        SELECT 1 FROM sys.foreign_keys 
        WHERE name = 'FK_reviews_booking'
    ) THEN '✅ Đã tạo' ELSE '❌ Chưa tạo' END
UNION ALL
SELECT 
    'Check Constraint CK_reviews_rating',
    CASE WHEN EXISTS (
        SELECT 1 FROM sys.check_constraints 
        WHERE name = 'CK_reviews_rating'
    ) THEN '✅ Đã tạo' ELSE '❌ Chưa tạo' END
UNION ALL
SELECT 
    'Index IX_reviews_booking_id',
    CASE WHEN EXISTS (
        SELECT 1 FROM sys.indexes 
        WHERE name = 'IX_reviews_booking_id'
    ) THEN '✅ Đã tạo' ELSE '❌ Chưa tạo' END
UNION ALL
SELECT 
    'Index IX_reviews_created_at',
    CASE WHEN EXISTS (
        SELECT 1 FROM sys.indexes 
        WHERE name = 'IX_reviews_created_at'
    ) THEN '✅ Đã tạo' ELSE '❌ Chưa tạo' END;

PRINT '';
PRINT '============================================';
PRINT '✅ HOÀN TẤT! Bảng reviews đã sẵn sàng.';
PRINT '============================================';
PRINT '';
PRINT 'Bây giờ hãy:';
PRINT '1. Restart backend server';
PRINT '2. Refresh trang web';
PRINT '3. Kiểm tra lại các API reviews';
PRINT '';

GO

