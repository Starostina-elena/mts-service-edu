package ru.aigul.mts_service.bpm;

import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import ru.aigul.mts_service.scheduler.ApplicationNotificationScheduler;

@Component("notifyManagersDelegate")
@RequiredArgsConstructor
public class NotifyManagersDelegate implements JavaDelegate {

    private final ApplicationNotificationScheduler scheduler;

    @Override
    public void execute(DelegateExecution execution) {
        scheduler.sendPendingApplicationsSummary();
    }
}
