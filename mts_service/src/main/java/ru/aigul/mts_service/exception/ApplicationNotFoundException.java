package ru.aigul.mts_service.exception;

public class ApplicationNotFoundException extends RuntimeException {
    public ApplicationNotFoundException(Long id) {
        super("Application not found: " + id);
    }
}
