package ru.aigul.mts_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class TariffAlreadyArchivedException extends RuntimeException {
    public TariffAlreadyArchivedException(Long id) {
        super("Tariff already archived: " + id);
    }
}

