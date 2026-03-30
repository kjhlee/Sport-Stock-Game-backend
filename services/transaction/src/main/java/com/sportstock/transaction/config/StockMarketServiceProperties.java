package com.sportstock.transaction.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "services.stock-market")
public class StockMarketServiceProperties {

  private String baseUrl;
  private int port;

  public String getFullUrl() {
    return baseUrl + ":" + port;
  }
}
