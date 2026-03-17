package com.sportstock.league;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.sportstock"})
public class LeagueApplication {

  public static void main(String[] args) {
    SpringApplication.run(LeagueApplication.class, args);
  }
}
