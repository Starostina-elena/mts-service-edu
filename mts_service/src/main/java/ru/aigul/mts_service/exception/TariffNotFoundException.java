package ru.aigul.mts_service.exception;

public class TariffNotFoundException extends RuntimeException {
    public TariffNotFoundException(Long id) {
        super("Tariff not found: " + id);
    }
}
