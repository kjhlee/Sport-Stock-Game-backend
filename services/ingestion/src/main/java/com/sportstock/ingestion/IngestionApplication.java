package com.sportstock.ingestion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.sportstock"})
public class IngestionApplication {

  public static void main(String[] args) {
    SpringApplication.run(IngestionApplication.class, args);
  }
}
