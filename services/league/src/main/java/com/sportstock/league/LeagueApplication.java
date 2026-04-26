package com.sportstock.league;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@SpringBootApplication(scanBasePackages = {"com.sportstock"})
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class LeagueApplication {

  public static void main(String[] args) {
    SpringApplication.run(LeagueApplication.class, args);
  }
}
