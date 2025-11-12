-- Add tax and service fee columns to bookings table
ALTER TABLE bookings
ADD subtotal_price DECIMAL(18, 2) NULL,
    tax_amount DECIMAL(18, 2) NULL,
    service_fee_amount DECIMAL(18, 2) NULL;

GO

-- Update existing bookings to set subtotal = total_price (for backward compatibility)
UPDATE bookings
SET subtotal_price = total_price,
    tax_amount = 0,
    service_fee_amount = 0
WHERE subtotal_price IS NULL;

