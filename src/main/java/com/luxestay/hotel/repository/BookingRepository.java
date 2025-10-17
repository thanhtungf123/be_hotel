package com.luxestay.hotel.repository;

import com.luxestay.hotel.model.entity.BookingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<BookingEntity, Integer> {}
