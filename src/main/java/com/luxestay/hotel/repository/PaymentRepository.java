package com.luxestay.hotel.repository;

import com.luxestay.hotel.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment,Integer> {
}
