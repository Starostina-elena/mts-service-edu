package ru.aigul.mts_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.aigul.mts_service.model.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
    Optional<User> findByName(String name);
}
