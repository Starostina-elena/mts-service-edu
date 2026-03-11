package ru.aigul.mts_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.aigul.mts_service.model.Application;
import ru.aigul.mts_service.model.User;
import ru.aigul.mts_service.repository.ApplicationRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final UserService userService;

    public List<Application> getApplicationsForUserEmail(String email) {
        Optional<User> userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty()) {
            return Collections.emptyList();
        }
        User user = userOpt.get();

        return applicationRepository.findAllByUserOrderByCreatedAtDesc(user);
    }
}
