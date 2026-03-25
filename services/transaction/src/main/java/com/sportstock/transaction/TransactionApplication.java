package com.sportstock.transaction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.sportstock"})
@EnableScheduling
public class TransactionApplication {

  public static void main(String[] args) {
    SpringApplication.run(TransactionApplication.class, args);
  }
}
