package com.sportstock.ingestion.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
    private String siteBaseUrl;

    @NotBlank
    private String coreBaseUrl;

    @NotBlank
    private String sport;

    @NotBlank
    private String league;

    @Min(1)
    private int defaultAthletePageSize = 100;

    @Min(1)
    private int defaultRosterLimit = 200;

    @Min(0)
    private int rateLimitDelayMs = 200;
}
