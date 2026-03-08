package com.example.user_authentication.service;

import java.time.OffsetDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.user_authentication.models.UserDetails;
import com.example.user_authentication.repository.UserAccountRepo;

@Service
public class RegisterAccountService {
    
    private final UserAccountRepo accountRepo;

    @Autowired
    BCryptPasswordEncoder passwordEncoder;

    public RegisterAccountService(UserAccountRepo accountRepo) {
        this.accountRepo = accountRepo;
    }
    // add the account to the database 
    public String registerAccount(String email, String password, String username, String firstname, String lastname) throws Exception{
        UserDetails newAccount = new UserDetails();
        newAccount.setEmail(email);
        newAccount.setUsername(username);
        newAccount.setFirstName(firstname);
        newAccount.setLastName(lastname);
        newAccount.setUpdateDate(OffsetDateTime.now());
        String hashedPassword = passwordEncoder.encode(password);
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
