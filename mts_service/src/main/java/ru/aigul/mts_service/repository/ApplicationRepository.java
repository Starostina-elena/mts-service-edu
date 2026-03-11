package ru.aigul.mts_service.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.aigul.mts_service.model.Application;
import ru.aigul.mts_service.model.ApplicationStatus;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    /**
     * Курсорная пагинация заявок с опциональной фильтрацией по статусу.
     * Вместо OFFSET используется keyset-подход (id > :after)
     */
    @Query("""
            SELECT a FROM Application a
            WHERE (:status IS NULL OR a.status = :status)
            AND (:after IS NULL OR a.id > :after)
            ORDER BY a.id ASC
            """)
    List<Application> findAll(@Param("status") ApplicationStatus status,
                              @Param("after") Long after,
                              Pageable pageable);
}
