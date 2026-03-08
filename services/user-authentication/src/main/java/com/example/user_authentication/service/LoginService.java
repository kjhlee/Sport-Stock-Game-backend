package com.example.user_authentication.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.user_authentication.DTO.TokenResponse;
import com.example.user_authentication.models.UserDetails;
import com.example.user_authentication.repository.UserAccountRepo;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final BCryptPasswordEncoder passwordEncoder;
    
    private final JwtService jwtService;

    private final UserAccountRepo accountRepo;

    public TokenResponse login(String login, String password) {
        UserDetails account;
        System.out.println("EXPIRED: " + jwtService.generateExpiredToken(login));

        if(login.contains("@")){
            account = accountRepo.findByEmail(login)
                    .orElseThrow(() -> new RuntimeException("Email not found"));
        }
        else {
            account = accountRepo.findByUsername(login)
                    .orElseThrow(() -> new RuntimeException("Username not found "));
        }
        if (!passwordEncoder.matches(password, account.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        String accessToken = jwtService.generateAccessToken(account.getEmail());
        String refreshToken = jwtService.generateRefreshToken(account.getEmail());

        return new TokenResponse(accessToken, refreshToken);
    }
}
