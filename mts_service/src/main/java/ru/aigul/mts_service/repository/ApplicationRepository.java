package ru.aigul.mts_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.aigul.mts_service.model.Application;
import ru.aigul.mts_service.model.User;

import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findAllByUserOrderByCreatedAtDesc(User user);
}

