package com.example.user_authentication.DTO;
import lombok.Data;

@Data
public class RegisterRequest {
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private String password;
}
