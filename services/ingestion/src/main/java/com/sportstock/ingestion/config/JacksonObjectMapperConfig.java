package com.sportstock.ingestion.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonObjectMapperConfig {

  @Bean
  public ObjectMapper ingestionObjectMapper() {
    return new ObjectMapper();
  }
}
