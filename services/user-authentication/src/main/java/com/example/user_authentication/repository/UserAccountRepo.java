package com.example.user_authentication.repository;

import com.example.user_authentication.models.UserDetails;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepo extends JpaRepository<UserDetails, UUID> {
  boolean existsByEmail(String email);

  boolean existsByUsername(String username);

  Optional<UserDetails> findByEmail(String email);

  Optional<UserDetails> findByUsername(String username);
}
