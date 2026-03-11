package ru.aigul.mts_service.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import ru.aigul.mts_service.exception.TariffNotFoundException;

import jakarta.validation.ConstraintViolationException;
import ru.aigul.mts_service.api.dto.ErrorResponse;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        err -> err.getField(),
                        err -> err.getDefaultMessage(),
                        (a, b) -> a + "; " + b
                ));

        ErrorResponse resp = new ErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, "validation failed", errors, OffsetDateTime.now(ZoneOffset.UTC));
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(resp);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        v -> v.getPropertyPath().toString(),
                        v -> v.getMessage(),
                        (a, b) -> a + "; " + b
                ));
        ErrorResponse resp = new ErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, "validation failed", errors, OffsetDateTime.now(ZoneOffset.UTC));
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(resp);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse resp = new ErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage() != null ? ex.getMessage() : "invalid request", Map.of(), OffsetDateTime.now(ZoneOffset.UTC));
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(resp);
    }
  
  @ExceptionHandler(TariffNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound(TariffNotFoundException ex) {
        return Map.of("error", ex.getMessage());
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadRequest(Exception ex) {
        return Map.of("error", ex.getMessage());
    }
}

