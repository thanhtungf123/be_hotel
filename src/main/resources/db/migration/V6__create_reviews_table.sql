-- Migration: Create reviews table
-- V6: Add review system for room bookings

USE [hotel_booking_system]
GO

-- Check if reviews table exists, if not create it
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
        CONSTRAINT [PK_reviews] PRIMARY KEY CLUSTERED ([review_id] ASC),
        CONSTRAINT [FK_reviews_booking] FOREIGN KEY ([booking_id]) 
            REFERENCES [dbo].[bookings]([booking_id]) ON DELETE CASCADE,
        CONSTRAINT [CK_reviews_rating] CHECK ([rating] >= 1 AND [rating] <= 5)
    );
    PRINT 'Created table: reviews';
END
ELSE
BEGIN
    PRINT 'Table reviews already exists';
END
GO

-- Create index for better query performance
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes 
    WHERE name = 'IX_reviews_booking_id' 
    AND object_id = OBJECT_ID('dbo.reviews')
)
BEGIN
    CREATE NONCLUSTERED INDEX [IX_reviews_booking_id] 
    ON [dbo].[reviews]([booking_id]) 
    INCLUDE ([rating], [created_at]);
    PRINT 'Added index: IX_reviews_booking_id';
END
ELSE
BEGIN
    PRINT 'Index IX_reviews_booking_id already exists';
END
GO

-- Create index for room reviews query
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes 
    WHERE name = 'IX_reviews_created_at' 
    AND object_id = OBJECT_ID('dbo.reviews')
)
BEGIN
    CREATE NONCLUSTERED INDEX [IX_reviews_created_at] 
    ON [dbo].[reviews]([created_at] DESC);
    PRINT 'Added index: IX_reviews_created_at';
END
ELSE
BEGIN
    PRINT 'Index IX_reviews_created_at already exists';
END
GO

PRINT 'Migration V6 completed successfully!';
GO

