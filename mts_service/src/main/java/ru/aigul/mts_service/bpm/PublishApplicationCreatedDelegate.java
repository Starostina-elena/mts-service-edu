package ru.aigul.mts_service.bpm;

import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import ru.aigul.mts_service.service.ApplicationService;

@Component("publishApplicationCreatedDelegate")
@RequiredArgsConstructor
public class PublishApplicationCreatedDelegate implements JavaDelegate {

    private final ApplicationService applicationService;

    @Override
    public void execute(DelegateExecution execution) {
        applicationService.publishCreatedEvent((Long) execution.getVariable("applicationId"));
    }
}
