package com.example.user_authentication.service;

import org.springframework.stereotype.Service;

import com.example.user_authentication.models.RegisterDetails;
import com.example.user_authentication.repository.RegisterAccountRepo;
import com.example.user_authentication.security.SecurityConfig;

@Service
public class RegisterAccountService {
    private final RegisterAccountRepo accountRepo;
    private final SecurityConfig passwordEncoder;

    public RegisterAccountService(RegisterAccountRepo accountRepo, SecurityConfig passwordEncoder) {
        this.accountRepo = accountRepo;
        this.passwordEncoder = passwordEncoder;
    }
    // add the account to the database 
    public String registerAccount(String email, String password) throws Exception{
        RegisterDetails newAccount = new RegisterDetails();
        newAccount.setEmail(email);

        String hashedPassword = passwordEncoder.passwordEncoder().encode(password);
        newAccount.setPassword(hashedPassword);
        if(newAccount.getEmail() == null || newAccount.getPassword() == null) {
            throw new Exception("Email or password cannot be null");
        }
        if(accountRepo.existsByEmail(newAccount.getEmail())) {
            throw new Exception("Email already exists");
        }
        accountRepo.save(newAccount);
        return "Account registered successfully";
    }
}
