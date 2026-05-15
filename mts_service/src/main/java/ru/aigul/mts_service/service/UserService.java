package ru.aigul.mts_service.service;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aigul.mts_service.dto.UserCreateRequest;
import ru.aigul.mts_service.exception.UserAlreadyExists;
import ru.aigul.mts_service.model.Role;
import ru.aigul.mts_service.model.User;
import ru.aigul.mts_service.repository.UserRepository;

import java.util.Collection;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User createUser(UserCreateRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new UserAlreadyExists(req.getEmail());
        }

        User u = new User();
        u.setEmail(req.getEmail());
        u.setName(req.getName());
        u.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        u.setRole(Role.USER);

        return userRepository.save(u);
    }

    public Optional<User> findByEmail(String emailOrName) {
        if (emailOrName == null) return Optional.empty();
        Optional<User> byEmail = userRepository.findByEmail(emailOrName);
        if (byEmail.isPresent()) return byEmail;
        return userRepository.findByName(emailOrName);
    }

    @Transactional
    public User findOrCreateFromAuthentication(Authentication auth) {
        if (auth == null) throw new IllegalArgumentException("Authentication required");
        String username = auth.getName();
        if (username == null) throw new IllegalArgumentException("Authentication principal has no name");

        Optional<User> opt = findByEmail(username);
        if (opt.isPresent()) return opt.get();

        if (!username.contains("@")) {
            Optional<User> alt = userRepository.findByEmail(username + "@test.com");
            if (alt.isPresent()) return alt.get();
        }

        User u = new User();
        u.setEmail(username);
        u.setName(username);
        u.setPasswordHash("");
        Role role = Role.USER;
        Collection<? extends GrantedAuthority> auths = auth.getAuthorities();
        if (auths != null) {
            for (GrantedAuthority ga : auths) {
                String a = ga.getAuthority();
                if ("ROLE_MANAGER".equals(a) || "MANAGER".equals(a)) { role = Role.MANAGER; break; }
                if ("ROLE_ADMIN".equals(a) || "ADMIN".equals(a)) { role = Role.ADMIN; break; }
            }
        }
        u.setRole(role);
        return userRepository.save(u);
    }
}