package com.example.user_authentication.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.user_authentication.models.RegisterDetails;

public interface RegisterAccountRepo extends JpaRepository<RegisterDetails, UUID> {
    boolean existsByEmail(String email);
}
