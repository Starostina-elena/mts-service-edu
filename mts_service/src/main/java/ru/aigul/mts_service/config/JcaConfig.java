package ru.aigul.mts_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.aigul.mts_service.jca.TaigaConnectionFactory;
import ru.aigul.mts_service.jca.TaigaConnectionFactoryImpl;

@Configuration
public class JcaConfig {

    @Bean
    public TaigaConnectionFactory taigaConnectionFactory() {
        return new TaigaConnectionFactoryImpl();
    }
}
