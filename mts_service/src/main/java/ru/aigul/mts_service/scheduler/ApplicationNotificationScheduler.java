package ru.aigul.mts_service.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.aigul.mts_service.kafka.ApplicationCreatedEvent;
import ru.aigul.mts_service.model.Application;
import ru.aigul.mts_service.model.ApplicationStatus;
import ru.aigul.mts_service.model.User;
import ru.aigul.mts_service.model.Role;
import ru.aigul.mts_service.repository.ApplicationRepository;
import ru.aigul.mts_service.repository.UserRepository;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ApplicationNotificationScheduler {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${notifications.enabled:true}")
    private boolean notificationsEnabled;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public ApplicationNotificationScheduler(ApplicationRepository applicationRepository, UserRepository userRepository) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
    }

    @Scheduled(fixedRateString = "${notifications.check-interval-ms:300000}")
    public void sendPendingApplicationsSummary() {
        if (!notificationsEnabled) {
            log.debug("Notifications disabled by configuration");
            return;
        }

        List<Application> pending = applicationRepository.findAllByStatus(ApplicationStatus.PENDING);
        if (pending == null || pending.isEmpty()) {
            log.debug("No pending applications found");
            return;
        }

        List<User> managers = userRepository.findAllByRole(Role.MANAGER);
        if (managers == null || managers.isEmpty()) {
            log.warn("No managers found to send notifications to");
            return;
        }

        String summary = buildSummary(pending);

        if (mailSender != null) {
            for (User m : managers) {
                try {
                    SimpleMailMessage msg = new SimpleMailMessage();
                    msg.setTo(m.getEmail());
                    msg.setSubject("Pending applications summary");
                    msg.setText(summary);
                    mailSender.send(msg);
                    log.info("Sent pending applications summary to {} ({} items)", m.getEmail(), pending.size());
                } catch (Exception e) {
                    log.error("Failed to send pending applications summary to {}", m.getEmail(), e);
                }
            }
        } else {
            log.info("Pending applications summary to managers (mail not configured):\n{}", summary);

            for (Application a : pending) {
                ApplicationCreatedEvent evt = new ApplicationCreatedEvent(
                        a.getId(),
                        a.getUser() != null ? a.getUser().getId() : null,
                        a.getTariff() != null ? a.getTariff().getId() : null,
                        a.getAddress(),
                        a.getCreatedAt() != null ? a.getCreatedAt().format(DATE_FMT) : null
                );
                try {
                    log.debug("Created event for application {}: userId={}", evt.getApplicationId(), evt.getUserId());
                } catch (Exception e) {
                    log.warn("Failed to build/log event for application {}", a.getId(), e);
                }
            }
        }
    }

    private String buildSummary(List<Application> apps) {
        String header = String.format("Pending applications count: %d\n\n", apps.size());
        String body = apps.stream().map(a -> String.format("id=%d userId=%s tariffId=%s createdAt=%s address=%s",
                a.getId(),
                a.getUser() != null ? String.valueOf(a.getUser().getId()) : "-",
                a.getTariff() != null ? String.valueOf(a.getTariff().getId()) : "-",
                a.getCreatedAt() != null ? a.getCreatedAt().format(DATE_FMT) : "-",
                a.getAddress() != null ? a.getAddress() : "-"
        )).collect(Collectors.joining("\n"));
        return header + body;
    }
}
