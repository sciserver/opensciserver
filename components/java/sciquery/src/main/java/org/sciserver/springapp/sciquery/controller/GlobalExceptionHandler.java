package org.sciserver.springapp.sciquery.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Global exception handler for validation errors across the API controller.
 * Provides error messages for constraint violations.
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOG = LogManager.getLogger();

    /**
     * Handles constraint violation exceptions from @Validated annotations.
     * Returns a detailed error message for each violated constraint.
     *
     * @param ex the ConstraintViolationException
     * @return ResponseEntity with HTTP 400 and detailed error message
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        LOG.warn("Constraint validation violation: {}", ex.getMessage());

        Map<String, Object> error = new HashMap<>();
        error.put("status", HttpStatus.BAD_REQUEST.value());
        error.put("error", ex.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .collect(Collectors.joining(". ")));
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles method argument validation errors from @Valid annotations on request body.
     * Returns a detailed error message for each field error.
     *
     * @param ex the MethodArgumentNotValidException
     * @return ResponseEntity with HTTP 400 and detailed error message
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        LOG.warn("Method argument validation failed: {}", ex.getMessage());

        Map<String, Object> error = new HashMap<>();
        error.put("status", HttpStatus.BAD_REQUEST.value());
        error.put("error", ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getDefaultMessage())
                .collect(Collectors.joining(". ")));
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
}
