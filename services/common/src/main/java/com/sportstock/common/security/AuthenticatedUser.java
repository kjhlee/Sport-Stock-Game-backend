package com.sportstock.common.security;

public record AuthenticatedUser (Long userId, String email) {
}