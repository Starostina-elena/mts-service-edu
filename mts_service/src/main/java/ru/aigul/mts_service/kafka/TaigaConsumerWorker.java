package ru.aigul.mts_service.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ru.aigul.mts_service.jca.TaigaConnection;
import ru.aigul.mts_service.jca.TaigaConnectionFactory;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "consumers.taiga", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TaigaConsumerWorker {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private KafkaConsumer<String, String> consumer;
    private Thread workerThread;

    private final TaigaConnectionFactory taigaConnectionFactory;

    @Value("${kafka.bootstrap:localhost:9092}")
    private String bootstrapServers;

    public TaigaConsumerWorker(TaigaConnectionFactory taigaConnectionFactory) {
        this.taigaConnectionFactory = taigaConnectionFactory;
    }

    @PostConstruct
    public void start() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "taiga-integration");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(List.of("applications.created"));

        workerThread = new Thread(this::runLoop, "taiga-consumer-thread");
        workerThread.setDaemon(true);
        workerThread.start();
        log.info("TaigaConsumerWorker started, bootstrap={}", bootstrapServers);
    }

    private void runLoop() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
                for (ConsumerRecord<String, String> r : records) {
                    try {
                        ApplicationCreatedEvent evt = objectMapper.readValue(r.value(), ApplicationCreatedEvent.class);
                        String subject = "New Application: " + evt.getApplicationId();
                        String description = String.format("TariffId=%s address=%s userId=%s", evt.getTariffId(), evt.getAddress(), evt.getUserId());
                        try (TaigaConnection conn = taigaConnectionFactory.getConnection()) {
                            conn.createIssue(subject, description);
                        }
                        log.info("TaigaConsumer processed applicationId={}", evt.getApplicationId());
                    } catch (Exception e) {
                        log.error("TaigaConsumer failed to process record key={} offset={}", r.key(), r.offset(), e);
                    }
                }
                try {
                    consumer.commitSync();
                } catch (Exception e) {
                    log.warn("Failed to commit offsets in TaigaConsumer", e);
                }
            }
        } catch (Exception e) {
            log.error("TaigaConsumerWorker loop terminated with error", e);
        } finally {
            try {
                consumer.close();
            } catch (Exception ignore) {}
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
        log.info("TaigaConsumerWorker stopped");
    }
}

