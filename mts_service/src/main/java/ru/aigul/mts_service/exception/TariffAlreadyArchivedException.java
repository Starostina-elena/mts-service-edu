package ru.aigul.mts_service.exception;

public class TariffAlreadyArchivedException extends RuntimeException {
    public TariffAlreadyArchivedException(Long id) {
        super("Tariff already archived: " + id);
    }
}

