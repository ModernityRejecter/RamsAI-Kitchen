package com.ramsai.kitchen.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/", "/index.html", "/login.html", "/register.html", "/profile.html", "/audit.html", "/tables.html", "/my-orders.html", "/error", "/static/**", "/css/**", "/js/**", "/images/**", "/uploads/**").permitAll()
                .requestMatchers("/api/v1/products/**").permitAll()
                .requestMatchers("/", "/index.html", "/menu.html", "/order.html", "/login.html", "/register.html", "/profile.html", "/audit.html", "/my-orders.html", "/error", "/static/**", "/css/**", "/js/**", "/images/**", "/uploads/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/tables/map").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/v1/tables/*/free").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/v1/tables/*/occupy").authenticated()
                .requestMatchers("/api/v1/manager/**").hasRole("MANAGER")
                .requestMatchers("/api/v1/kitchen/**").hasAnyRole("CHEF", "MANAGER")
                .requestMatchers("/api/v1/ai/**").hasAnyRole("CHEF", "MANAGER")
                .requestMatchers("/api/v1/tables/**").hasAnyRole("WAITER", "MANAGER")
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authenticationProvider(authenticationProvider)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
