package ru.aigul.mts_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.aigul.mts_service.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
}
