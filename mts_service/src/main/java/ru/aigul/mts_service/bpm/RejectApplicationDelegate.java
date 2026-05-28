package ru.aigul.mts_service.bpm;

import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import ru.aigul.mts_service.dto.application.ApplicationRejectDto;
import ru.aigul.mts_service.service.ApplicationService;

@Component("rejectApplicationDelegate")
@RequiredArgsConstructor
public class RejectApplicationDelegate implements JavaDelegate {

    private final ApplicationService applicationService;

    @Override
    public void execute(DelegateExecution execution) {
        applicationService.reject(
                (Long) execution.getVariable("managerUserId"),
                (Long) execution.getVariable("applicationId"),
                new ApplicationRejectDto((String) execution.getVariable("rejectReason"))
        );
    }
}
