package com.sportstock.stockmarket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.sportstock"})
public class StockMarketApplication {

  public static void main(String[] args) {
    SpringApplication.run(StockMarketApplication.class, args);
  }
}
