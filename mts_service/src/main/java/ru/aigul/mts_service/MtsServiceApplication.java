package ru.aigul.mts_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MtsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MtsServiceApplication.class, args);
    }

}
