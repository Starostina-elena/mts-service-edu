package ru.aigul.mts_service.exception;

public class InvalidApplicationStatusException extends RuntimeException {
    public InvalidApplicationStatusException(String message) {
        super(message);
    }
}
