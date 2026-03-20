package ru.aigul.mts_service.service;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aigul.mts_service.exception.UserAlreadyExists;
import ru.aigul.mts_service.model.Role;
import ru.aigul.mts_service.model.User;
import ru.aigul.mts_service.repository.UserRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User createUser(String email, String name, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExists(email);
        }

        User u = new User();
        u.setEmail(email);
        u.setName(name);
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setRole(Role.USER);

        return userRepository.save(u);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
