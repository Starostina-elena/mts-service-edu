package ru.aigul.mts_service.bpm;

import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import ru.aigul.mts_service.dto.application.ApplicationCreateDto;
import ru.aigul.mts_service.dto.application.ApplicationDto;
import ru.aigul.mts_service.service.ApplicationService;

import java.util.Arrays;
import java.util.List;

@Component("createApplicationDelegate")
@RequiredArgsConstructor
public class CreateApplicationDelegate implements JavaDelegate {

    private final ApplicationService applicationService;

    @Override
    public void execute(DelegateExecution execution) {
        Long userId = (Long) execution.getVariable("userId");
        Long tariffId = (Long) execution.getVariable("tariffId");
        Long cityId = (Long) execution.getVariable("cityId");
        String address = (String) execution.getVariable("address");
        String additionalServiceIds = (String) execution.getVariable("additionalServiceIds");

        ApplicationDto application = applicationService.createApplicationRecord(
                userId,
                new ApplicationCreateDto(tariffId, address, cityId, parseIds(additionalServiceIds))
        );
        execution.setVariable("applicationId", application.getId());
    }

    private List<Long> parseIds(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .toList();
    }
}
