package com.luxestay.hotel.repository;

import com.luxestay.hotel.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Integer> {
    Optional<Account> findByEmail(String email);
    boolean existsByEmail(String email);

    Optional<Account> findById(Integer id);

    List<Account> findAllByRole_Id(Integer id);

    // OAuth
    Optional<Account> findByProviderAndProviderId(String provider, String providerId);
    Optional<Account> findByEmailAndProvider(String email, String provider);
}
