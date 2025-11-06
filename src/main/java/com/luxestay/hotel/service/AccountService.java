package com.luxestay.hotel.service;

import com.luxestay.hotel.model.Account;
import com.luxestay.hotel.repository.AccountRepository;
import com.luxestay.hotel.repository.RoleRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;

    public List<Account> findAll() {
        return accountRepository.findAll();
    }

    public Account findById(int id) {
        return accountRepository.findById(id).orElse(null);
    }
    @Transactional
    public void  save(Account account) {
//        account.setRole(roleRepository.findById(1).orElse(null));
        accountRepository.save(account);
    }

    @Transactional
    public void  saveCreate(Account account) {
        account.setRole(roleRepository.findById(1).orElse(null));
        accountRepository.save(account);
    }
    @Transactional
    public void delete(int id) {
        Account account = accountRepository.findById(id).orElse(null);
        if (account != null) {
            account.setIsActive(false);
            accountRepository.save(account);
        }
    }

    public List<Account> getAllbyRoleId(int id) {
        return accountRepository.findAllByRole_Id(id);
    }


}
