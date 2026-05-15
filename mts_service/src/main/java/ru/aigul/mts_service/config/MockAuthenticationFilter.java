package ru.aigul.mts_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.userdetails.User;

import java.io.IOException;
import java.util.List;

@Component
public class MockAuthenticationFilter extends OncePerRequestFilter {

    @Value("${app.auth.mock-header}")
    private String mockHeader;

    @Value("${app.auth.default-user}")
    private String defaultUser;

    @Value("${app.auth.manager-user}")
    private String managerUser;

    @Value("${app.auth.admin-user}")
    private String adminUser;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() != null && SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && !authHeader.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        String mockUser = request.getHeader(mockHeader);
        if (mockUser == null || mockUser.isBlank()) {
            mockUser = defaultUser;
        }

        if (mockUser == null || mockUser.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        UsernamePasswordAuthenticationToken auth;
        if (mockUser.equals(managerUser)) {
            User principal = new User(mockUser, "", List.of(new SimpleGrantedAuthority("MANAGER")));
            auth = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of(new SimpleGrantedAuthority("MANAGER"))
            );
        } else if (mockUser.equals(adminUser)) {
            User principal = new User(mockUser, "", List.of(new SimpleGrantedAuthority("ADMIN")));
            auth = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of(new SimpleGrantedAuthority("ADMIN"))
            );
        } else {
            User principal = new User(mockUser, "", List.of(new SimpleGrantedAuthority("USER")));
            auth = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of(new SimpleGrantedAuthority("USER"))
            );
        }

        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }
}