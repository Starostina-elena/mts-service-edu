package ru.aigul.mts_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class MockAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String mockUser = request.getHeader("X-Mock-User");
        if (mockUser == null || mockUser.isBlank()) {
            mockUser = "mock@local";
        }

        UsernamePasswordAuthenticationToken auth;
        if (mockUser.equals("manager@local")) {
            auth = new UsernamePasswordAuthenticationToken(
                    mockUser,
                    null,
                    List.of(new SimpleGrantedAuthority("MANAGER"))
            );
        } else if (mockUser.equals("admin@local")) {
            auth = new UsernamePasswordAuthenticationToken(
                    mockUser,
                    null,
                    List.of(new SimpleGrantedAuthority("ADMIN"))
            );
        } else {
            auth = new UsernamePasswordAuthenticationToken(
                    mockUser,
                    null,
                    List.of(new SimpleGrantedAuthority("USER"))
            );
        }

        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }
}

