package ru.aigul.mts_service.config;

import org.camunda.bpm.engine.FormService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.spring.ProcessEngineFactoryBean;
import org.camunda.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.camunda.bpm.engine.spring.components.jobexecutor.SpringJobExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import ru.aigul.mts_service.bpm.ApproveApplicationDelegate;
import ru.aigul.mts_service.bpm.CreateApplicationDelegate;
import ru.aigul.mts_service.bpm.NotifyManagersDelegate;
import ru.aigul.mts_service.bpm.PublishApplicationCreatedDelegate;
import ru.aigul.mts_service.bpm.RejectApplicationDelegate;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class CamundaConfig {

    @Bean
    public SpringJobExecutor camundaJobExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(2);
        taskExecutor.setMaxPoolSize(4);
        taskExecutor.setQueueCapacity(20);
        taskExecutor.setThreadNamePrefix("camunda-");
        taskExecutor.initialize();

        SpringJobExecutor executor = new SpringJobExecutor();
        executor.setTaskExecutor(taskExecutor);
        return executor;
    }

    @Bean
    public SpringProcessEngineConfiguration processEngineConfiguration(
            @Qualifier("primaryDataSource") DataSource dataSource,
            @Qualifier("transactionManager") PlatformTransactionManager transactionManager,
            ApplicationContext applicationContext,
            ResourcePatternResolver resourcePatternResolver,
            SpringJobExecutor camundaJobExecutor,
            CreateApplicationDelegate createApplicationDelegate,
            PublishApplicationCreatedDelegate publishApplicationCreatedDelegate,
            ApproveApplicationDelegate approveApplicationDelegate,
            RejectApplicationDelegate rejectApplicationDelegate,
            NotifyManagersDelegate notifyManagersDelegate,
            @Value("${app.camunda.deploy-processes:true}") boolean deployProcesses) throws IOException {
        SpringProcessEngineConfiguration configuration = new SpringProcessEngineConfiguration();
        configuration.setDataSource(dataSource);
        configuration.setTransactionManager(transactionManager);
        configuration.setDatabaseSchemaUpdate(ProcessEngineConfigurationImpl.DB_SCHEMA_UPDATE_TRUE);
        configuration.setJobExecutor(camundaJobExecutor);
        configuration.setJobExecutorActivate(true);
        configuration.setHistory(ProcessEngineConfigurationImpl.HISTORY_AUDIT);
        configuration.setApplicationContext(applicationContext);
        Map<Object, Object> beans = new HashMap<>();
        beans.put("createApplicationDelegate", createApplicationDelegate);
        beans.put("publishApplicationCreatedDelegate", publishApplicationCreatedDelegate);
        beans.put("approveApplicationDelegate", approveApplicationDelegate);
        beans.put("rejectApplicationDelegate", rejectApplicationDelegate);
        beans.put("notifyManagersDelegate", notifyManagersDelegate);
        configuration.setBeans(beans);
        if (deployProcesses) {
            List<org.springframework.core.io.Resource> resources = new ArrayList<>();
            resources.addAll(List.of(resourcePatternResolver.getResources("classpath*:bpmn/*.bpmn")));
            resources.addAll(List.of(resourcePatternResolver.getResources("classpath*:forms/*.form")));
            configuration.setDeploymentResources(resources.toArray(org.springframework.core.io.Resource[]::new));
        }
        return configuration;
    }

    @Bean
    public ProcessEngineFactoryBean processEngineFactoryBean(SpringProcessEngineConfiguration configuration) {
        ProcessEngineFactoryBean factoryBean = new ProcessEngineFactoryBean();
        factoryBean.setProcessEngineConfiguration(configuration);
        return factoryBean;
    }

    @Bean
    public RepositoryService repositoryService(ProcessEngine processEngine) {
        return processEngine.getRepositoryService();
    }

    @Bean
    public RuntimeService runtimeService(ProcessEngine processEngine) {
        return processEngine.getRuntimeService();
    }

    @Bean
    public TaskService taskService(ProcessEngine processEngine) {
        return processEngine.getTaskService();
    }

    @Bean
    public FormService formService(ProcessEngine processEngine) {
        return processEngine.getFormService();
    }
}
