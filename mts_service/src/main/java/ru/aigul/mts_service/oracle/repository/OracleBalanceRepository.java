package ru.aigul.mts_service.oracle.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.aigul.mts_service.oracle.model.BalanceOracle;

import java.util.Optional;

public interface OracleBalanceRepository extends JpaRepository<BalanceOracle, Long> {

    Optional<BalanceOracle> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BalanceOracle b where b.userId = :userId")
    Optional<BalanceOracle> findByUserIdForUpdate(@Param("userId") Long userId);
}

