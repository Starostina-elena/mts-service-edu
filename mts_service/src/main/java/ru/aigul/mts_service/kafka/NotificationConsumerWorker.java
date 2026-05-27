package ru.aigul.mts_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import ru.aigul.mts_service.repository.UserRepository;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "consumers.notifications", name = "enabled", havingValue = "true", matchIfMissing = true)
public class NotificationConsumerWorker {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private KafkaConsumer<String, String> consumer;
    private Thread workerThread;

    private final UserRepository userRepository;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${kafka.bootstrap:localhost:9092}")
    private String bootstrapServers;

    public NotificationConsumerWorker(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void start() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "notifications");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(List.of("applications.created"));

        workerThread = new Thread(this::runLoop, "notifications-consumer-thread");
        workerThread.setDaemon(true);
        workerThread.start();
        log.info("NotificationConsumerWorker started, bootstrap={}", bootstrapServers);
    }

    private void runLoop() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
                for (ConsumerRecord<String, String> r : records) {
                    try {
                        ApplicationCreatedEvent evt = objectMapper.readValue(r.value(), ApplicationCreatedEvent.class);
                        Optional<ru.aigul.mts_service.model.User> uOpt = userRepository.findById(evt.getUserId());
                        String email = uOpt.map(ru.aigul.mts_service.model.User::getEmail).orElse(null);
                        String body = String.format("Your application %s has been received and is pending processing.", evt.getApplicationId());
                        if (email != null && mailSender != null) {
                            SimpleMailMessage msg = new SimpleMailMessage();
                            msg.setTo(email);
                            msg.setSubject("Application received");
                            msg.setText(body);
                            try {
                                mailSender.send(msg);
                                log.info("Sent notification email to {} for application {}", email, evt.getApplicationId());
                            } catch (Exception e) {
                                log.error("Failed to send email to {} for application {}", email, evt.getApplicationId(), e);
                            }
                        } else {
                            log.info("Notification: userEmail={} body={}", email, body);
                        }
                    } catch (Exception e) {
                        log.error("NotificationConsumer failed to process record key={} offset={}", r.key(), r.offset(), e);
                    }
                }
                try {
                    consumer.commitSync();
                } catch (Exception e) {
                    log.warn("Failed to commit offsets in NotificationConsumer", e);
                }
            }
        } catch (Exception e) {
            log.error("NotificationConsumerWorker loop terminated with error", e);
        } finally {
            try { consumer.close(); } catch (Exception ignore) {}
        }
    }

    @PreDestroy
    public void stop() {
        if (workerThread != null) {
            workerThread.interrupt();
            try { workerThread.join(2000); } catch (InterruptedException ignored) {}
        }
        if (consumer != null) {
            try { consumer.wakeup(); } catch (Exception ignored) {}
            try { consumer.close(); } catch (Exception ignored) {}
        }
        log.info("NotificationConsumerWorker stopped");
    }
}

