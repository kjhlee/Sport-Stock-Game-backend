package com.sportstock.common.security;

public record JwtUserPrincipal(String email, Long userId, String username) {}
