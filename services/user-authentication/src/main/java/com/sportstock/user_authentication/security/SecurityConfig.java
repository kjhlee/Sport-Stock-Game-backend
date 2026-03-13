package com.sportstock.user_authentication.security;

import com.sportstock.common.security.BaseSecurityConfig;
import com.sportstock.common.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig extends BaseSecurityConfig {

  @Autowired JwtAuthenticationFilter jwtAuthenticationFilter;

  @Bean
  public BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    applyBaseSecurityConfig(http, jwtAuthenticationFilter);
    http.authorizeHttpRequests(
        auth ->
            auth.requestMatchers("/api/register/**", "/api/login/**", "/api/refresh/**")
                .permitAll()
                .anyRequest()
                .authenticated());

    return http.build();
  }
}
