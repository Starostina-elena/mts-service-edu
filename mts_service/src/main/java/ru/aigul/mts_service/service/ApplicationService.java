package ru.aigul.mts_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aigul.mts_service.dto.CursorPage;
import ru.aigul.mts_service.dto.application.*;
import ru.aigul.mts_service.exception.ApplicationNotFoundException;
import ru.aigul.mts_service.exception.InsufficientFundsException;
import ru.aigul.mts_service.exception.InvalidApplicationStatusException;
import ru.aigul.mts_service.exception.AccessDeniedException;
import ru.aigul.mts_service.exception.TariffNotFoundException;
import ru.aigul.mts_service.exception.UserNotFoundException;
import ru.aigul.mts_service.mapper.ApplicationMapper;
import ru.aigul.mts_service.model.*;
import ru.aigul.mts_service.balance.model.Balance;
import ru.aigul.mts_service.balance.repository.BalanceRepository;
import ru.aigul.mts_service.repository.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;

import java.util.Collections;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final TariffRepository tariffRepository;
    private final TariffCityPriceRepository tariffCityPriceRepository;
    private final UserRepository userRepository;
    private final BalanceRepository balanceRepository;
    private final ServiceRepository serviceRepository;
    private final ApplicationMapper applicationMapper;
    private final UserService userService;

    public List<Application> getApplicationsForUserEmail(String email) {
        Optional<User> userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty()) {
            return Collections.emptyList();
        }
        User user = userOpt.get();

        return applicationRepository.findAllByUserOrderByCreatedAtDesc(user);
    }

    @Transactional
    public ApplicationDto create(Long userId, ApplicationCreateDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Tariff tariff = tariffRepository.findById(dto.getTariffId())
                .orElseThrow(() -> new TariffNotFoundException(dto.getTariffId()));

        BigDecimal tariffPrice = tariff.getBasePrice();
        if (tariffPrice == null && dto.getCityId() != null) {
            tariffPrice = tariffCityPriceRepository
                    .findByTariffIdAndCityId(dto.getTariffId(), dto.getCityId())
                    .map(TariffCityPrice::getPrice)
                    .orElse(BigDecimal.ZERO);
        }
        if (tariffPrice == null) {
            tariffPrice = BigDecimal.ZERO;
        }
        BigDecimal totalPrice = tariffPrice;

        List<ru.aigul.mts_service.model.Service> additionalServices = List.of();
        if (!dto.getAdditionalServiceIds().isEmpty()) {
            additionalServices = serviceRepository.findAllById(dto.getAdditionalServiceIds());
            for (ru.aigul.mts_service.model.Service s : additionalServices) {
                totalPrice = totalPrice.add(s.getPrice());
            }
        }

        Balance balance = balanceRepository.findByUserId(userId)
                .orElseThrow(() -> new InsufficientFundsException());
        if (balance.getAmount().compareTo(totalPrice) < 0) {
            throw new InsufficientFundsException();
        }

        Application application = new Application();
        application.setUser(user);
        application.setTariff(tariff);
        application.setAddress(dto.getAddress());
        application.setStatus(ApplicationStatus.PENDING);
        application.setAdditionalServices(new HashSet<>(additionalServices));

        application = applicationRepository.save(application);
        return applicationMapper.toDto(application);
    }

    @Transactional(readOnly = true)
    public CursorPage<ApplicationDto> list(Long userId, ApplicationStatus status, Long after, int limit) {
        List<Application> raw = applicationRepository.findAllFiltered(userId, status, after, Limit.of(limit + 1));
        return CursorPage.of(raw, limit, applicationMapper::toDto, Application::getId);
    }

    @Transactional(readOnly = true)
    public ApplicationDetailDto getDetail(Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));
        return applicationMapper.toDetailDto(application);
    }

    @Transactional
    public ApplicationDto approve(Long userId, Long applicationId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        if (user.getRole() != Role.MANAGER) {
            throw new AccessDeniedException("Only manager can approve applications");
        }

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));

        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new InvalidApplicationStatusException("Application already processed");
        }

        BigDecimal totalPrice = BigDecimal.ZERO;
        Tariff tariff = application.getTariff();
        if (tariff != null && tariff.getBasePrice() != null) {
            totalPrice = tariff.getBasePrice();
        }
        if (application.getAdditionalServices() != null) {
            for (ru.aigul.mts_service.model.Service s : application.getAdditionalServices()) {
                if (s != null && s.getPrice() != null) totalPrice = totalPrice.add(s.getPrice());
            }
        }

        Long applUserId = application.getUser().getId();
        Balance balance = balanceRepository.findByUserId(applUserId)
                .orElseThrow(() -> new InsufficientFundsException());
        if (balance.getAmount().compareTo(totalPrice) < 0) {
            throw new InsufficientFundsException();
        }
        balance.setAmount(balance.getAmount().subtract(totalPrice));
        balanceRepository.save(balance);

        application.setStatus(ApplicationStatus.APPROVED);
        application = applicationRepository.save(application);
        return applicationMapper.toDto(application);
    }
    @Transactional
    public ApplicationDto reject(Long userId, Long applicationId, ApplicationRejectDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        if (user.getRole() != Role.MANAGER) {
            throw new AccessDeniedException("Only manager can reject applications");
        }

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));

        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new InvalidApplicationStatusException("Application already processed");
        }

        application.setStatus(ApplicationStatus.REJECTED);
        application.setRejectReason(dto.getReason());
        application = applicationRepository.save(application);
        return applicationMapper.toDto(application);
    }
}
