package com.ramsai.kitchen.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index.html", "/error", "/static/**", "/css/**", "/js/**", "/images/**").permitAll()
                .anyRequest().permitAll()
            )
            .formLogin(login -> login
                .defaultSuccessUrl("/", true)
                .permitAll()
            )
            .logout(logout -> logout.permitAll());

        return http.build();
    }
}
