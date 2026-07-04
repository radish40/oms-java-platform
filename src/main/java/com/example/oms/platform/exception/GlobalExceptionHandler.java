package com.example.oms.platform.exception;

import com.example.oms.platform.dto.response.ErrorResponse;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> business(BusinessException exception) {
        return ResponseEntity.status(exception.getStatus())
                .header("cache-control", "no-store")
                .body(ErrorResponse.of(exception.getCode(), exception.getMessage(), exception.getDetails()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> accessDenied(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .header("cache-control", "no-store")
                .body(ErrorResponse.of("FORBIDDEN", exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException exception) {
        return ResponseEntity.badRequest()
                .header("cache-control", "no-store")
                .body(ErrorResponse.of("BAD_REQUEST", exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unexpected(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("cache-control", "no-store")
                .body(ErrorResponse.of("INTERNAL_ERROR", exception.getMessage(), Map.of()));
    }
}
