package com.example.agenticstorage.controller;

import com.example.agenticstorage.model.McpDtos.McpResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<McpResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(McpResponse.error("Validation failed: " + errors));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<McpResponse<Void>> handleSecurity(SecurityException ex) {
        return ResponseEntity.status(403).body(McpResponse.error("SANDBOX VIOLATION: " + ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<McpResponse<Void>> handleGeneral(Exception ex) {
        return ResponseEntity.internalServerError().body(McpResponse.error(ex.getMessage()));
    }
}
