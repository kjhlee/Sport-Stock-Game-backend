package com.sportstock.common.dto.league;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CreateLeagueRequest(
    @NotBlank @Size(max = 255) String name,
    @NotNull @Min(2) Integer maxMembers,
    @NotNull @Future OffsetDateTime seasonStartAt,
    @NotNull @Future OffsetDateTime seasonEndAt,
    @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal initialStipendAmount,
    @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal weeklyStipendAmount) {

  public CreateLeagueRequest {
    if (seasonStartAt != null && seasonEndAt != null && !seasonEndAt.isAfter(seasonStartAt)) {
      throw new IllegalArgumentException("seasonEndAt must be after seasonStartAt");
    }
  }
}
