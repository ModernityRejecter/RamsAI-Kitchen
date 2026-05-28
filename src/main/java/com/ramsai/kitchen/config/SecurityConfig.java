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
                // 1. Public Auth endpoints
                .requestMatchers("/api/v1/auth/**").permitAll()
                // 2. Public Static Resources
                .requestMatchers(
                    "/", 
                    "/*.html", 
                    "/static/**", 
                    "/css/**", 
                    "/js/**", 
                    "/images/**", 
                    "/uploads/**", 
                    "/error"
                ).permitAll()

                // Tables public (authenticated) endpoints
                .requestMatchers(HttpMethod.GET, "/api/v1/tables/map").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/v1/tables/*/free").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/v1/tables/*/occupy").authenticated()

                // 3. Manager/Chef management endpoints (MUST be before public /api/v1/products/**)
                .requestMatchers(HttpMethod.PATCH, "/api/v1/products/*/approve").hasRole("MANAGER")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/products/*/reject").hasRole("MANAGER")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/products/*/active").hasRole("MANAGER")
                .requestMatchers(HttpMethod.GET, "/api/v1/products/manage").hasAnyRole("MANAGER", "CHEF")
                .requestMatchers(HttpMethod.POST, "/api/v1/products/**").hasAnyRole("MANAGER", "CHEF")
                .requestMatchers(HttpMethod.PUT, "/api/v1/products/**").hasAnyRole("MANAGER", "CHEF")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/products/**").hasAnyRole("MANAGER", "CHEF")

                // 4. Other Manager specific endpoints
                .requestMatchers("/api/v1/manager/**").hasRole("MANAGER")
                .requestMatchers(HttpMethod.POST, "/api/v1/inventory/**").hasRole("MANAGER")
                .requestMatchers(HttpMethod.PUT, "/api/v1/inventory/**").hasRole("MANAGER")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/inventory/**").hasRole("MANAGER")
                .requestMatchers("/api/v1/inventory/logs/**").hasRole("MANAGER")
                
                // 5. Chef/Manager shared endpoints
                .requestMatchers("/api/v1/kitchen/**").hasAnyRole("CHEF", "MANAGER")
                .requestMatchers("/api/v1/ai/**").hasAnyRole("CHEF", "MANAGER")
                .requestMatchers("/api/v1/inventory/**").hasAnyRole("CHEF", "MANAGER")
                
                // 6. Waiter/Manager shared endpoints
                .requestMatchers("/api/v1/tables/**").hasAnyRole("WAITER", "MANAGER")
                
                // 7. Public API endpoints (remaining product gets, etc.)
                .requestMatchers("/api/v1/products/**").permitAll()
                
                // 8. Everything else requires authentication
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
