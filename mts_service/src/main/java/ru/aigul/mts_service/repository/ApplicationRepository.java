package ru.aigul.mts_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.aigul.mts_service.model.Application;
import ru.aigul.mts_service.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.aigul.mts_service.model.ApplicationStatus;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findAllByUserOrderByCreatedAtDesc(User user);

    @Query("""
            SELECT a FROM Application a
            WHERE (:status IS NULL OR a.status = :status)
            AND (:after IS NULL OR a.id > :after)
            ORDER BY a.id ASC
            """)
    List<Application> findAll(@Param("status") ApplicationStatus status,
                              @Param("after") Long after,
                              Pageable pageable);

    @Query("""
            SELECT a FROM Application a
            WHERE a.id > :after
            ORDER BY a.id ASC
            """)
    List<Application> findAllAfter(@Param("after") Long after, Pageable pageable);

    @Query("""
            SELECT a FROM Application a
            WHERE a.status = :status AND a.id > :after
            ORDER BY a.id ASC
            """)
    List<Application> findAllByStatusAfter(@Param("status") ApplicationStatus status,
                                           @Param("after") Long after,
                                           Pageable pageable);

    @Query("SELECT a FROM Application a WHERE a.status = :status ORDER BY a.id ASC")
    List<Application> findAllByStatus(@Param("status") ApplicationStatus status, Pageable pageable);

    @Query("SELECT a FROM Application a ORDER BY a.id ASC")
    List<Application> findAllOrdered(Pageable pageable);

    @Query("SELECT a FROM Application a WHERE a.user.id = :userId ORDER BY a.id ASC")
    List<Application> findAllByUser(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT a FROM Application a WHERE a.user.id = :userId AND a.id > :after ORDER BY a.id ASC")
    List<Application> findAllByUserAfter(@Param("userId") Long userId, @Param("after") Long after, Pageable pageable);

    @Query("SELECT a FROM Application a WHERE a.user.id = :userId AND a.status = :status ORDER BY a.id ASC")
    List<Application> findAllByUserAndStatus(@Param("userId") Long userId, @Param("status") ApplicationStatus status, Pageable pageable);

    @Query("SELECT a FROM Application a WHERE a.user.id = :userId AND a.status = :status AND a.id > :after ORDER BY a.id ASC")
    List<Application> findAllByUserAndStatusAfter(@Param("userId") Long userId, @Param("status") ApplicationStatus status, @Param("after") Long after, Pageable pageable);
}
