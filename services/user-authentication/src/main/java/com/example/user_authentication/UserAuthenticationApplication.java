package com.example.user_authentication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
    scanBasePackages = {"com.sportstock.common.security", "com.example.user_authentication"})
public class UserAuthenticationApplication {

  public static void main(String[] args) {
    SpringApplication.run(UserAuthenticationApplication.class, args);
  }
}
