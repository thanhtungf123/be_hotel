package com.luxestay.hotel.repository;

import com.luxestay.hotel.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(String name); // "customer", "admin", ...
}
