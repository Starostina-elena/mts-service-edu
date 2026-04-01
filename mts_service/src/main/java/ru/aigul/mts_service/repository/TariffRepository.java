package ru.aigul.mts_service.repository;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.aigul.mts_service.model.Tariff;
import ru.aigul.mts_service.model.TariffCategory;
import ru.aigul.mts_service.model.TariffStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface TariffRepository extends JpaRepository<Tariff, Long> {

    @Query("""
            SELECT t FROM Tariff t
            WHERE t.status = :status AND t.category = :category
            AND (:after IS NULL OR t.id > :after)
            ORDER BY t.id ASC
            """)
    List<Tariff> findByStatusAndCategoryWithServices(@Param("status") TariffStatus status,
                                                      @Param("category") TariffCategory category,
                                                      @Param("after") Long after,
                                                      Limit limit);

    // Для HOME: фильтр по city через tariff_city_prices + опциональный priceMax
    @Query("""
            SELECT DISTINCT t FROM Tariff t
            JOIN TariffCityPrice tcp ON tcp.tariff = t AND tcp.city.id = :cityId
            WHERE t.status = 'ACTIVE' AND t.category = 'HOME'
            AND (:priceMax IS NULL OR tcp.price <= :priceMax)
            AND (:after IS NULL OR t.id > :after)
            ORDER BY t.id ASC
            """)
    List<Tariff> findHome(@Param("cityId") Long cityId,
                          @Param("priceMax") BigDecimal priceMax,
                          @Param("after") Long after,
                          Limit limit);

    // Для LANDLINE и SATELLITE_TV: фильтр по city через tariff_city_prices
    @Query("""
            SELECT DISTINCT t FROM Tariff t
            JOIN TariffCityPrice tcp ON tcp.tariff = t AND tcp.city.id = :cityId
            WHERE t.status = 'ACTIVE' AND t.category = :category
            AND (:after IS NULL OR t.id > :after)
            ORDER BY t.id ASC
            """)
    List<Tariff> findByCity(@Param("category") TariffCategory category,
                            @Param("cityId") Long cityId,
                            @Param("after") Long after,
                            Limit limit);

    // Загружает services одним JOIN FETCH запросом для списка тарифов (решение N+1)
    @Query("SELECT DISTINCT t FROM Tariff t LEFT JOIN FETCH t.services WHERE t IN :tariffs ORDER BY t.id ASC")
    List<Tariff> fetchWithServices(@Param("tariffs") List<Tariff> tariffs);

    // Загружает одиночный тариф с services (для detail-страницы)
    @Query("SELECT t FROM Tariff t LEFT JOIN FETCH t.services WHERE t.id = :id")
    Optional<Tariff> findByIdWithServices(@Param("id") Long id);

    // Для MOBILE: опциональные фильтры
    @Query("""
            SELECT t FROM Tariff t
            WHERE t.status = 'ACTIVE' AND t.category = 'MOBILE'
            AND (:internetGb IS NULL OR t.internetGb >= :internetGb)
            AND (:callsMinutes IS NULL OR t.callsMinutes >= :callsMinutes)
            AND (:forFamily IS NULL OR t.forFamily = :forFamily)
            AND (:noSubscriptionFee IS NULL OR t.noSubscriptionFee = :noSubscriptionFee)
            AND (:after IS NULL OR t.id > :after)
            ORDER BY t.id ASC
            """)
    List<Tariff> findMobile(@Param("internetGb") Integer internetGb,
                            @Param("callsMinutes") Integer callsMinutes,
                            @Param("forFamily") Boolean forFamily,
                            @Param("noSubscriptionFee") Boolean noSubscriptionFee,
                            @Param("after") Long after,
                            Limit limit);
}
