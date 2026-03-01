package com.sportstock.ingestion.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "espn.api")
public class EspnApiProperties {

    @NotBlank
    @Pattern(regexp = "^https://[^\\s]+$")
    private String siteBaseUrl;

    @NotBlank
    @Pattern(regexp = "^https://[^\\s]+$")
    private String coreBaseUrl;

    @NotBlank
    private String sport;

    @NotBlank
    private String league;

    @Min(1)
    private int defaultAthletePageSize = 250;

    @Min(1)
    private int defaultRosterLimit = 200;

    @Min(0)
    private int rateLimitDelayMs = 200;

    @Min(100)
    @Max(500000)
    private int maxAthleteRowsPerSync = 50000;

    @Min(1)
    @Max(60)
    private int connectTimeoutSeconds = 5;

    @Min(1)
    @Max(300)
    private int readTimeoutSeconds = 60;

    @Min(5)
    @Max(1800)
    private int athletePageTransactionTimeoutSeconds = 120;
}
