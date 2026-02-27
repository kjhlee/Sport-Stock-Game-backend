package com.example.user_authentication.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.user_authentication.DTO.RegisterRequest;
import com.example.user_authentication.service.RegisterAccountService;

@RestController
@RequestMapping("/api/register")
public class RegisterController {
    
    private RegisterAccountService accountService;

    public RegisterController(RegisterAccountService accountService) {
        this.accountService = accountService;
    }
    
    @PostMapping
    public ResponseEntity<String> registerAccount(@RequestBody RegisterRequest request) {
        try {
            accountService.registerAccount(request.getEmail(), request.getPassword());
            return ResponseEntity.ok("Account registered successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
