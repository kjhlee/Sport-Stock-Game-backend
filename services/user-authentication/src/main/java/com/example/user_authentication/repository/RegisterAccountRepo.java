package com.example.user_authentication.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.user_authentication.models.RegisterDetails;

public interface RegisterAccountRepo extends JpaRepository<RegisterDetails, UUID> {
    boolean existsByEmail(String email);
    Optional<RegisterDetails> findByEmail(String email);
}
