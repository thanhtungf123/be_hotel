-- Migration: Create booking_services join table (many-to-many)
-- V5: Add relationship between bookings and services

USE [hotel_booking_system]
GO

-- Check if booking_services table exists, if not create it
IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.TABLES 
    WHERE TABLE_SCHEMA = 'dbo' 
    AND TABLE_NAME = 'booking_services'
)
BEGIN
    CREATE TABLE booking_services (
        booking_id INT NOT NULL,
        service_id INT NOT NULL,
        CONSTRAINT PK_booking_services PRIMARY KEY (booking_id, service_id),
        CONSTRAINT FK_booking_services_booking FOREIGN KEY (booking_id) 
            REFERENCES bookings(booking_id) ON DELETE CASCADE,
        CONSTRAINT FK_booking_services_service FOREIGN KEY (service_id) 
            REFERENCES services(service_id) ON DELETE CASCADE
    );
    PRINT 'Created table: booking_services';
END
ELSE
BEGIN
    PRINT 'Table booking_services already exists';
END
GO

-- Add index for better query performance
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes 
    WHERE name = 'IX_booking_services_booking_id' 
    AND object_id = OBJECT_ID('dbo.booking_services')
)
BEGIN
    CREATE INDEX IX_booking_services_booking_id 
    ON booking_services(booking_id);
    PRINT 'Added index: IX_booking_services_booking_id';
END
ELSE
BEGIN
    PRINT 'Index IX_booking_services_booking_id already exists';
END
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes 
    WHERE name = 'IX_booking_services_service_id' 
    AND object_id = OBJECT_ID('dbo.booking_services')
)
BEGIN
    CREATE INDEX IX_booking_services_service_id 
    ON booking_services(service_id);
    PRINT 'Added index: IX_booking_services_service_id';
END
ELSE
BEGIN
    PRINT 'Index IX_booking_services_service_id already exists';
END
GO

PRINT 'Migration V5 completed successfully!';
GO


