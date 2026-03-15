package com.sportstock.user_authentication.repository;

import com.sportstock.user_authentication.models.UserDetails;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepo extends JpaRepository<UserDetails, Long> {
  boolean existsByEmail(String email);

  boolean existsByUsername(String username);

  Optional<UserDetails> findByEmail(String email);

  Optional<UserDetails> findByUsername(String username);
}
