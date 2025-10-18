package com.luxestay.hotel.service;

import com.luxestay.hotel.model.Account;
import com.luxestay.hotel.repository.AccountRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;


import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    public List<Account> findAll() {
        return accountRepository.findAll();
    }

    public Account findById(int id) {
        return accountRepository.findById(id).orElse(null);
    }

    public void  save(Account account) {
        accountRepository.save(account);
    }


    public void delete(int id) {
        Account account = accountRepository.findById(id).orElse(null);
        if (account != null) {
            account.setIsActive(false);
            accountRepository.save(account);
        }
    }
}
