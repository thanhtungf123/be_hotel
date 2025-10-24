package com.luxestay.hotel.repository;

import com.luxestay.hotel.model.entity.BookingCustomerDetails;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingCustomerDetailsRepository extends JpaRepository<BookingCustomerDetails, Integer> { }
