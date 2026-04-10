package com.sportstock.user_authentication.service;

import com.sportstock.user_authentication.exception.RegistrationConflictException;
import com.sportstock.user_authentication.models.UserDetails;
import com.sportstock.user_authentication.repository.UserAccountRepo;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class RegisterAccountService {
  private final UserAccountRepo accountRepo;

  @Autowired BCryptPasswordEncoder passwordEncoder;

  public RegisterAccountService(UserAccountRepo accountRepo) {
    this.accountRepo = accountRepo;
  }

  // add the account to the database
  public String registerAccount(
      String email, String password, String username, String firstname, String lastname) {
    UserDetails newAccount = new UserDetails();
    newAccount.setEmail(email);
    newAccount.setUsername(username);
    newAccount.setFirstName(firstname);
    newAccount.setLastName(lastname);
    newAccount.setUpdateDate(OffsetDateTime.now());
    String hashedPassword = passwordEncoder.encode(password);
    newAccount.setPassword(hashedPassword);

    if (newAccount.getEmail() == null || newAccount.getPassword() == null) {
      throw new IllegalArgumentException("Email or password cannot be null");
    }

    Map<String, String> fieldErrors = new HashMap<>();
    if (accountRepo.existsByUsername(newAccount.getUsername())) {
      fieldErrors.put("username", "Username is already taken.");
    }
    if (accountRepo.existsByEmail(newAccount.getEmail())) {
      fieldErrors.put("email", "Email is already registered.");
    }
    if (!fieldErrors.isEmpty()) {
      throw new RegistrationConflictException("Please fix the highlighted fields.", fieldErrors);
    }

    accountRepo.save(newAccount);
    return "Account registered successfully";
  }
}
