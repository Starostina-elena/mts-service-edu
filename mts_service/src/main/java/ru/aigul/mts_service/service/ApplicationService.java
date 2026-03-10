package ru.aigul.mts_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.aigul.mts_service.api.dto.ApplicationResponse;
import ru.aigul.mts_service.model.Application;
import ru.aigul.mts_service.model.User;
import ru.aigul.mts_service.repository.ApplicationRepository;

import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final UserService userService;

    public List<ApplicationResponse> getApplicationsForUserEmail(String email) {
        Optional<User> userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty()) {
            return Collections.emptyList();
        }
        User user = userOpt.get();

        List<Application> apps = applicationRepository.findAllByUserOrderByCreatedAtDesc(user);

        return apps.stream().map(app -> new ApplicationResponse(
                app.getId().toString(),
                user.getId().toString(),
                app.getTariff() != null ? app.getTariff().getId().toString() : null,
                app.getTariff() != null ? app.getTariff().getName() : null,
                app.getAddress(),
                app.getStatus() != null ? app.getStatus().name().toLowerCase() : null,
                app.getRejectReason(),
                app.getCreatedAt() != null ? app.getCreatedAt().atOffset(ZoneOffset.UTC).toString() : null,
                app.getUpdatedAt() != null ? app.getUpdatedAt().atOffset(ZoneOffset.UTC).toString() : null
        )).collect(Collectors.toList());
    }
}

