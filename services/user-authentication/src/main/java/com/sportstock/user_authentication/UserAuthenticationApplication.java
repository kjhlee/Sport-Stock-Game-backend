package com.sportstock.user_authentication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.sportstock"})
public class UserAuthenticationApplication {

  public static void main(String[] args) {
    SpringApplication.run(UserAuthenticationApplication.class, args);
  }
}
