package com.sportstock.transaction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.sportstock"})
public class TransactionApplication {

  public static void main(String[] args) {
    SpringApplication.run(TransactionApplication.class, args);
  }
}
