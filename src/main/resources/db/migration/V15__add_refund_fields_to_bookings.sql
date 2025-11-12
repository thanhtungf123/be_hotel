-- Add refund amount and percent fields to bookings table
ALTER TABLE bookings
ADD refund_amount DECIMAL(18, 2) NULL,
    refund_percent INT NULL;

