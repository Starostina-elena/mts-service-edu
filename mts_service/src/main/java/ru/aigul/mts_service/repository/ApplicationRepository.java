package ru.aigul.mts_service.repository;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.aigul.mts_service.model.Application;
import ru.aigul.mts_service.model.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.aigul.mts_service.model.ApplicationStatus;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findAllByUserOrderByCreatedAtDesc(User user);

    @Query("""
            SELECT a FROM Application a
            JOIN FETCH a.user JOIN FETCH a.tariff
            WHERE (:userId IS NULL OR a.user.id = :userId)
            AND a.status = COALESCE(:status, a.status)
            AND (:after IS NULL OR a.id > :after)
            ORDER BY a.id ASC
            """)
    List<Application> findAllFiltered(@Param("userId") Long userId,
                                      @Param("status") ApplicationStatus status,
                                      @Param("after") Long after,
                                      Limit limit);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Application a WHERE a.id = :id")
    Optional<Application> findByIdForUpdate(@Param("id") Long id);
}
