package com.sportstock.common.dto.league;

import jakarta.validation.constraints.NotBlank;

public record UpdateInitialStipendStatusRequest(@NotBlank String status) {}
