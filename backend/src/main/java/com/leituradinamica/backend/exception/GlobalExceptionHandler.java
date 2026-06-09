package com.leituradinamica.backend.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException exception, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler({BusinessException.class, ConstraintViolationException.class})
    public ResponseEntity<Map<String, Object>> handleBusiness(Exception exception, HttpServletRequest request) {
        HttpStatus status = exception instanceof BusinessException businessException
                ? businessException.getStatus()
                : HttpStatus.BAD_REQUEST;
        return build(status, exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleAuth(BadCredentialsException exception, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Credenciais inválidas.", request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Requisição inválida.");
        return build(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception exception, HttpServletRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), request.getRequestURI());
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message, String path) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", Instant.now(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message,
                "path", path
        ));
    }
}