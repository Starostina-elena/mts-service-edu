package ru.aigul.mts_service.exception;

public class UserAlreadyExists extends RuntimeException {
    public UserAlreadyExists(String email) {
        super("User with email " + email + " already exists");
    }
}
