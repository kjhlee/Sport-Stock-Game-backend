package com.sportstocks.stockmarket.config;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@ConfigurationProperties(prefix = "pricing")
@Getter
@Setter
public class PricingConfig {
    private BigDecimal smoothingAlpha;
    private BigDecimal priceFloor;
    private Map<String, BigDecimal> basePrices;
}