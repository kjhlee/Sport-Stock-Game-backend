package com.example.user_authentication.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.user_authentication.models.RegisterDetails;
import com.example.user_authentication.service.RegisterAccountService;

@RestController
@RequestMapping("/api/register")
public class RegisterController {
    
    private RegisterAccountService accountService;

    public RegisterController(RegisterAccountService accountService) {
        this.accountService = accountService;
    }
    
    @PostMapping
    public ResponseEntity<String> registerAccount(@RequestBody RegisterDetails newAccount) {
        try {
            accountService.registerAccount(newAccount);
            return ResponseEntity.ok("Account registered successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
