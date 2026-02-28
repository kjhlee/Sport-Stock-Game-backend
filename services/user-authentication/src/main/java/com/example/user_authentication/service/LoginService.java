package com.example.user_authentication.service;

import org.springframework.stereotype.Service;

import com.example.user_authentication.models.RegisterDetails;
import com.example.user_authentication.repository.RegisterAccountRepo;
import com.example.user_authentication.security.SecurityConfig;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final RegisterAccountRepo accountRepo;
    private final SecurityConfig sc;


    public void login(String email, String password) {
        RegisterDetails account = accountRepo.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        if (sc.passwordEncoder().matches(password, account.getPassword())) {
            // Authentication successful
            System.out.println("Login successful!");
        } else {
            throw new RuntimeException("Invalid credentials");
        }
    }
}
