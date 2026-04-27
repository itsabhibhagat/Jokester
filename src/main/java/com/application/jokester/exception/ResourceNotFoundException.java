package com.application.jokester.exception;

// Thrown when any entity is not found in the database.
// Maps to HTTP 404 Not Found in the GlobalExceptionHandler.
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}