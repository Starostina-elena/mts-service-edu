package ru.aigul.mts_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${app.security.public-post-endpoints}")
    private String publicPostEndpoints;

    @Value("${app.security.public-endpoints}")
    private String publicEndpoints;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, MockAuthenticationFilter mockFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests((authz) -> authz
                        .requestMatchers(HttpMethod.POST, publicPostEndpoints).permitAll()
                        .requestMatchers(publicEndpoints).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(mockFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}