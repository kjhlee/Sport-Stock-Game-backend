package com.example.user_authentication.service;

import org.springframework.stereotype.Service;

import com.example.user_authentication.models.UserDetails;
import com.example.user_authentication.repository.UserAccountRepo;
import com.example.user_authentication.security.SecurityConfig;

@Service
public class RegisterAccountService {
    
    private final UserAccountRepo accountRepo;
    private final SecurityConfig passwordEncoder;

    public RegisterAccountService(UserAccountRepo accountRepo, SecurityConfig passwordEncoder) {
        this.accountRepo = accountRepo;
        this.passwordEncoder = passwordEncoder;
    }
    // add the account to the database 
    public String registerAccount(String email, String password) throws Exception{
        UserDetails newAccount = new UserDetails();
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
