package ru.aigul.mts_service.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Properties;

@Service
@Slf4j
public class KafkaProducerService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Producer<String, String> producer;

    @Value("${kafka.bootstrap:localhost:9092}")
    private String bootstrapServers;

    private final String topic = "applications.created";

    @PostConstruct
    public void init() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        producer = new KafkaProducer<>(props);
        log.info("KafkaProducerService initialized, bootstrap={} topic={}", bootstrapServers, topic);
    }

    public void sendApplicationCreated(ApplicationCreatedEvent evt) {
        try {
            String key = evt.getUserId() != null ? String.valueOf(evt.getUserId()) : null;
            String value = objectMapper.writeValueAsString(evt);
            ProducerRecord<String, String> rec = new ProducerRecord<>(topic, key, value);
            producer.send(rec, (metadata, ex) -> {
                if (ex != null) {
                    log.error("Failed to send application.created event: {}", ex.getMessage(), ex);
                } else {
                    log.info("Sent application.created event: topic={} partition={} offset={} key={}", metadata.topic(), metadata.partition(), metadata.offset(), key);
                }
            });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize ApplicationCreatedEvent", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        if (producer != null) {
            try {
                producer.flush();
                producer.close();
            } catch (Exception e) {
                log.warn("Error while closing Kafka producer", e);
            }
        }
    }
}
