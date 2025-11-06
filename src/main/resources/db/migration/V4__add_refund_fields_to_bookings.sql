-- Migration: Add refund information fields to bookings table
-- V4: Add refund fields for cancellation refund process

USE [hotel_booking_system]
GO

-- Check and add refund_account_holder column
IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = 'dbo' 
    AND TABLE_NAME = 'bookings' 
    AND COLUMN_NAME = 'refund_account_holder'
)
BEGIN
    ALTER TABLE bookings ADD refund_account_holder NVARCHAR(255) NULL;
    PRINT 'Added column: refund_account_holder';
END
ELSE
BEGIN
    PRINT 'Column refund_account_holder already exists';
END
GO

-- Check and add refund_account_number column
IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = 'dbo' 
    AND TABLE_NAME = 'bookings' 
    AND COLUMN_NAME = 'refund_account_number'
)
BEGIN
    ALTER TABLE bookings ADD refund_account_number NVARCHAR(50) NULL;
    PRINT 'Added column: refund_account_number';
END
ELSE
BEGIN
    PRINT 'Column refund_account_number already exists';
END
GO

-- Check and add refund_bank_name column
IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = 'dbo' 
    AND TABLE_NAME = 'bookings' 
    AND COLUMN_NAME = 'refund_bank_name'
)
BEGIN
    ALTER TABLE bookings ADD refund_bank_name NVARCHAR(255) NULL;
    PRINT 'Added column: refund_bank_name';
END
ELSE
BEGIN
    PRINT 'Column refund_bank_name already exists';
END
GO

-- Check and add refund_submitted_at column
IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = 'dbo' 
    AND TABLE_NAME = 'bookings' 
    AND COLUMN_NAME = 'refund_submitted_at'
)
BEGIN
    ALTER TABLE bookings ADD refund_submitted_at DATETIME2 NULL;
    PRINT 'Added column: refund_submitted_at';
END
ELSE
BEGIN
    PRINT 'Column refund_submitted_at already exists';
END
GO

-- Check and add refund_completed_at column
IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = 'dbo' 
    AND TABLE_NAME = 'bookings' 
    AND COLUMN_NAME = 'refund_completed_at'
)
BEGIN
    ALTER TABLE bookings ADD refund_completed_at DATETIME2 NULL;
    PRINT 'Added column: refund_completed_at';
END
ELSE
BEGIN
    PRINT 'Column refund_completed_at already exists';
END
GO

-- Check and add refund_completed_by column
IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = 'dbo' 
    AND TABLE_NAME = 'bookings' 
    AND COLUMN_NAME = 'refund_completed_by'
)
BEGIN
    ALTER TABLE bookings ADD refund_completed_by INT NULL;
    PRINT 'Added column: refund_completed_by';
END
ELSE
BEGIN
    PRINT 'Column refund_completed_by already exists';
END
GO

-- Check and add foreign key constraint for refund_completed_by
IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS 
    WHERE CONSTRAINT_SCHEMA = 'dbo' 
    AND TABLE_NAME = 'bookings' 
    AND CONSTRAINT_NAME = 'FK_bookings_refund_completed_by'
)
BEGIN
    ALTER TABLE bookings
    ADD CONSTRAINT FK_bookings_refund_completed_by 
        FOREIGN KEY (refund_completed_by) 
        REFERENCES accounts(account_id);
    PRINT 'Added foreign key: FK_bookings_refund_completed_by';
END
ELSE
BEGIN
    PRINT 'Foreign key FK_bookings_refund_completed_by already exists';
END
GO

-- Check and add index for querying bookings with refund info
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes 
    WHERE name = 'IX_bookings_refund_submitted' 
    AND object_id = OBJECT_ID('dbo.bookings')
)
BEGIN
    CREATE INDEX IX_bookings_refund_submitted 
    ON bookings(refund_submitted_at) 
    WHERE refund_submitted_at IS NOT NULL;
    PRINT 'Added index: IX_bookings_refund_submitted';
END
ELSE
BEGIN
    PRINT 'Index IX_bookings_refund_submitted already exists';
END
GO

PRINT 'Migration V4 completed successfully!';
GO

