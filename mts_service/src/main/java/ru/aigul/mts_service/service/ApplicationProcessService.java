package ru.aigul.mts_service.service;

import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Service;
import ru.aigul.mts_service.dto.application.ApplicationCreateDto;
import ru.aigul.mts_service.dto.application.ApplicationDto;
import ru.aigul.mts_service.dto.application.ApplicationRejectDto;
import ru.aigul.mts_service.exception.ApplicationNotFoundException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationProcessService {

    private static final String PROCESS_KEY = "applicationHandling";
    private static final String MANAGER_TASK = "managerDecision";

    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final ApplicationService applicationService;

    public ApplicationDto create(Long userId, ApplicationCreateDto dto) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("userId", userId);
        variables.put("tariffId", dto.getTariffId());
        variables.put("cityId", dto.getCityId());
        variables.put("address", dto.getAddress());
        variables.put("additionalServiceIds", (dto.getAdditionalServiceIds() == null ? java.util.List.<Long>of() : dto.getAdditionalServiceIds()).stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",")));

        List<ProcessDefinition> definitions = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(PROCESS_KEY)
                .withoutTenantId()
                .orderByProcessDefinitionVersion()
                .desc()
                .listPage(0, 1);
        if (definitions.isEmpty()) {
            throw new ProcessEngineException("Process definition not found: " + PROCESS_KEY);
        }
        ProcessDefinition definition = definitions.get(0);
        ProcessInstance instance = runtimeService.startProcessInstanceById(definition.getId(), variables);
        Long applicationId = (Long) runtimeService.getVariable(instance.getId(), "applicationId");
        return applicationService.getDto(applicationId);
    }

    public ApplicationDto approve(Long managerUserId, Long applicationId) {
        Task task = managerTask(applicationId);
        taskService.complete(task.getId(), Map.of(
                "approved", true,
                "managerUserId", managerUserId
        ));
        return applicationService.getDto(applicationId);
    }

    public ApplicationDto reject(Long managerUserId, Long applicationId, ApplicationRejectDto dto) {
        Task task = managerTask(applicationId);
        taskService.complete(task.getId(), Map.of(
                "approved", false,
                "managerUserId", managerUserId,
                "rejectReason", dto.getReason()
        ));
        return applicationService.getDto(applicationId);
    }

    private Task managerTask(Long applicationId) {
        Task task = taskService.createTaskQuery()
                .processVariableValueEquals("applicationId", applicationId)
                .taskDefinitionKey(MANAGER_TASK)
                .active()
                .singleResult();
        if (task == null) {
            throw new ApplicationNotFoundException(applicationId);
        }
        return task;
    }
}
