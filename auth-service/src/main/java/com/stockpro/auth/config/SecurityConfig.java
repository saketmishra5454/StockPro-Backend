package com.stockpro.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SecurityConfig - configures Spring Security for auth-service.
 *
 * Key decisions:
 * 1. CSRF disabled - not needed for REST APIs (stateless, no browser sessions)
 * 2. All requests permitted - JWT validation is done at the api-gateway level,
 *    not inside each microservice. The gateway acts as the security checkpoint.
 * 3. Stateless session - no HTTP session stored on server (JWT handles state)
 * 4. BCryptPasswordEncoder - industry standard for password hashing
 *
 * Why BCrypt?
 * BCrypt automatically salts passwords (adds random data before hashing)
 * so two users with the same password get different hashes.
 * It's intentionally slow to make brute-force attacks impractical.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF - not needed for stateless REST APIs
                .csrf(AbstractHttpConfigurer::disable)

                // Stateless - no server-side sessions (JWT carries all state)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Allow all requests - JWT validation done at api-gateway level
                .authorizeHttpRequests(auth ->
                        auth.anyRequest().permitAll());

        return http.build();
    }

    /**
     * BCryptPasswordEncoder bean - injected into AuthServiceImpl.
     * Strength 10 = default, good balance of security vs performance.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}