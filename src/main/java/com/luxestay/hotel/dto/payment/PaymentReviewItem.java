// com/luxestay/hotel/dto/payment/PaymentReviewItem.java
package com.luxestay.hotel.dto.payment;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PaymentReviewItem {
    public Integer bookingId;
    public String roomName;
    public LocalDate checkIn;
    public LocalDate checkOut;
    public BigDecimal totalPrice;
    public BigDecimal depositAmount;
    public BigDecimal amountPaid;
    public String paymentState; // deposit_paid | paid_in_full
}
