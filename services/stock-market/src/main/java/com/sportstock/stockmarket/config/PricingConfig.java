package com.sportstock.stockmarket.config;

import java.math.BigDecimal;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "pricing")
@Getter
@Setter
public class PricingConfig {
  private BigDecimal priceFloor;
  private Map<String, BigDecimal> basePrices;
  private Map<String, BigDecimal> multipliers;
}
