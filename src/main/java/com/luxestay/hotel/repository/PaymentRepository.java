package com.luxestay.hotel.repository;

import com.luxestay.hotel.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface PaymentRepository extends JpaRepository<Payment,Integer> {

    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.booking.id = :bookingId and lower(p.status)='completed'")
    BigDecimal sumPaidByBooking(@Param("bookingId") Integer bookingId);
}
