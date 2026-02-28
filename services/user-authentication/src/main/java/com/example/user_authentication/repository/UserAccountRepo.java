package com.example.user_authentication.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.user_authentication.models.UserDetails;

public interface UserAccountRepo extends JpaRepository<UserDetails, UUID> {
    boolean existsByEmail(String email);
    Optional<UserDetails> findByEmail(String email);
}
