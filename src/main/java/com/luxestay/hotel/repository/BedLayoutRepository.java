package com.luxestay.hotel.repository;

import com.luxestay.hotel.model.entity.BedLayout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BedLayoutRepository extends JpaRepository<BedLayout, Integer> {
    Optional<BedLayout> findByLayoutName(String layoutName);
}
