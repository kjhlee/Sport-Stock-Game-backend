package com.example.user_authentication.service;

import org.springframework.stereotype.Service;

import com.example.user_authentication.models.RegisterDetails;
import com.example.user_authentication.repository.RegisterAccountRepo;

@Service
public class RegisterAccountService {
    private final RegisterAccountRepo accountRepo;

    public RegisterAccountService(RegisterAccountRepo accountRepo) {
        this.accountRepo = accountRepo;
    }
    // add the account to the database 
    public String registerAccount(RegisterDetails newAccount) throws Exception{
        if(accountRepo.existsByEmail(newAccount.getEmail())) {
            throw new Exception("Email already exists");
        }
        accountRepo.save(newAccount);
        return "Account registered successfully";
    }
}
