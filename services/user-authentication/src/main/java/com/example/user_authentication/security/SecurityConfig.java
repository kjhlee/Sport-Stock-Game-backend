package com.example.user_authentication.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // for APIs + Postman during dev
            .csrf(csrf -> csrf.disable())

            // if you're building a stateless REST API
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // authorize endpoints
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/register/**", "/api/login/**").permitAll()
                .anyRequest().authenticated()
            )

            // during dev you can use basic auth (optional)
            .httpBasic(Customizer.withDefaults());

        return http.build();
    }

}
